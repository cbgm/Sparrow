package com.cbgm.sparrow.feature.transport.websocket

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionState
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayBlobUploadTicket
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayBlobUploadTicketRequest
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayClientMessage
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayEnvelopeAcceptance
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayTypingEvent
import com.cbgm.sparrow.feature.transport.gateway.model.TransportEnvelope
import com.cbgm.sparrow.feature.transport.presence.ClientPresenceRouteCoordinator
import com.cbgm.sparrow.feature.transport.presence.PresenceRouteConnection
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

@OptIn(InternalSerializationApi::class)
class DefaultWebSocketTransportClient internal constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val presenceRouteCoordinator: ClientPresenceRouteCoordinator,
    private val serverMessageHandler: GatewayServerMessageHandler,
    private val pendingRequestRegistry: GatewayPendingRequestRegistry
) : WebSocketTransportClient {
    private val logger = SparrowLog.withTag("DefaultWebSocketTransportClient")
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val mutableConnectionState =
        MutableStateFlow<TransportConnectionState>(TransportConnectionState.Disconnected)
    override val connectionState: StateFlow<TransportConnectionState> =
        mutableConnectionState.asStateFlow()

    private val mutableRegisteredRoutingAliases = MutableStateFlow<Set<String>>(emptySet())

    private val mutableIncomingEnvelopes =
        MutableSharedFlow<TransportEnvelope>(extraBufferCapacity = INCOMING_BUFFER_CAPACITY)
    override val incomingEnvelopes: Flow<TransportEnvelope> = mutableIncomingEnvelopes.asSharedFlow()

    private val mutableIncomingTypingEvents =
        MutableSharedFlow<GatewayTypingEvent>(extraBufferCapacity = INCOMING_BUFFER_CAPACITY)
    override val incomingTypingEvents: Flow<GatewayTypingEvent> =
        mutableIncomingTypingEvents.asSharedFlow()

    private val sessionMutex = Mutex()
    private val sendMutex = Mutex()

    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null

    override fun connect(
        serverUrl: String,
        localRoutingId: String
    ) {
        require(serverUrl.isNotBlank()) {
            "Gateway WebSocket URL must not be blank"
        }
        require(localRoutingId.isNotBlank()) {
            "Local routing ID must not be blank"
        }
        if (connectionJob?.isActive == true) return

        connectionJob =
            clientScope.launch {
                runConnection(
                    serverUrl = serverUrl,
                    localRoutingId = localRoutingId
                )
            }
    }

    override suspend fun sendEnvelopeAndAwaitAcceptance(
        envelope: TransportEnvelope,
        timeoutMilliseconds: Long
    ): Result<Unit> =
        sendEnvelopeAndAwaitServerAcceptance(envelope, timeoutMilliseconds).map { Unit }

    override suspend fun sendEnvelopeAndAwaitServerAcceptance(
        envelope: TransportEnvelope,
        timeoutMilliseconds: Long
    ): Result<GatewayEnvelopeAcceptance> =
        awaitEnvelopeAcceptance(
            envelopeId = envelope.envelopeId,
            timeoutMilliseconds = timeoutMilliseconds
        ) {
            sendClientMessage(GatewayClientMessage.SendEnvelope(envelope))
        }

    override suspend fun sendFederatedEnvelopeAndAwaitAcceptance(
        envelope: FederatedEnvelope,
        timeoutMilliseconds: Long
    ): Result<Unit> =
        sendFederatedEnvelopeAndAwaitServerAcceptance(envelope, timeoutMilliseconds).map { Unit }

    override suspend fun sendFederatedEnvelopeAndAwaitServerAcceptance(
        envelope: FederatedEnvelope,
        timeoutMilliseconds: Long
    ): Result<GatewayEnvelopeAcceptance> =
        awaitEnvelopeAcceptance(
            envelopeId = envelope.envelopeId,
            timeoutMilliseconds = timeoutMilliseconds
        ) {
            sendClientMessage(GatewayClientMessage.SendFederatedEnvelope(envelope))
        }

    override suspend fun requestBlobUploadTicket(
        request: GatewayBlobUploadTicketRequest,
        timeoutMilliseconds: Long
    ): Result<GatewayBlobUploadTicket> {
        if (connectionState.value !is TransportConnectionState.Connected) {
            return Result.failure(IllegalStateException("WebSocket transport is not connected"))
        }

        return pendingRequestRegistry.awaitBlobUploadTicket(
            requestId = request.requestId,
            timeoutMilliseconds = timeoutMilliseconds
        ) {
            sendClientMessage(
                GatewayClientMessage.RequestBlobUploadTicket(
                    requestId = request.requestId,
                    blobId = request.blobId,
                    maximumBytes = request.maximumBytes,
                    readCapabilitySha256 = request.readCapabilitySha256,
                    deleteCapabilitySha256 = request.deleteCapabilitySha256,
                    blobExpiresAtEpochMilliseconds = request.blobExpiresAtEpochMilliseconds
                )
            )
        }
    }

    override suspend fun awaitRoutingAlias(
        routingAlias: String,
        timeoutMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            require(routingAlias.isNotBlank()) {
                "Routing alias must not be blank"
            }
            require(timeoutMilliseconds > 0L) {
                "Routing alias timeout must be positive"
            }
            withTimeout(timeoutMilliseconds.milliseconds) {
                mutableRegisteredRoutingAliases.first { aliases -> routingAlias in aliases }
            }
        }

    override suspend fun acknowledgeIncomingEnvelope(envelopeId: String): Result<Unit> =
        runCatching {
            require(envelopeId.isNotBlank()) {
                "Envelope ID must not be blank"
            }
            sendClientMessage(
                GatewayClientMessage.AcknowledgeEnvelope(envelopeId = envelopeId)
            )
        }

    override suspend fun sendTypingState(
        recipientId: String,
        isTyping: Boolean
    ): Result<Unit> =
        runCatching {
            require(recipientId.isNotBlank()) {
                "Recipient routing ID must not be blank"
            }
            check(connectionState.value is TransportConnectionState.Connected) {
                "WebSocket transport is not connected"
            }
            sendClientMessage(
                GatewayClientMessage.TypingState(
                    recipientId = recipientId,
                    isTyping = isTyping
                )
            )
        }

    override suspend fun disconnect() {
        val activeConnectionJob = connectionJob
        connectionJob = null

        val activeSession =
            sessionMutex.withLock {
                session.also { session = null }
            }

        runCatching {
            activeSession?.close(
                reason =
                    CloseReason(
                        code = CloseReason.Codes.NORMAL,
                        message = "Client disconnect"
                    )
            )
        }

        activeConnectionJob?.cancelAndJoin()
        pendingRequestRegistry.failAll(
            IllegalStateException("WebSocket disconnected")
        )
        mutableRegisteredRoutingAliases.value = emptySet()
        mutableConnectionState.value = TransportConnectionState.Disconnected
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth")
    private suspend fun runConnection(
        serverUrl: String,
        localRoutingId: String
    ) {
        mutableConnectionState.value = TransportConnectionState.Connecting

        val connectionId = IdGenerator.generate()
        val generation = SystemClock.nowEpochMilliseconds()

        try {
            httpClient.webSocket(urlString = serverUrl) {
                sessionMutex.withLock {
                    session = this
                }

                logger.info { "WebSocket session opened" }
                sendRegistration(
                    localRoutingId = localRoutingId,
                    connectionId = connectionId
                )

                val presenceSession =
                    presenceRouteCoordinator.createSession(
                        scope = this,
                        connection =
                            PresenceRouteConnection(
                                serverUrl = serverUrl,
                                routingId = localRoutingId,
                                connectionId = connectionId,
                                generation = generation
                            ),
                        publishRoute = { registration ->
                            runCatching {
                                sendClientMessage(
                                    GatewayClientMessage.RefreshRoute(registration)
                                )
                            }
                        },
                        onReady = { aliases ->
                            mutableRegisteredRoutingAliases.value = aliases
                            mutableConnectionState.value =
                                TransportConnectionState.Connected(routingId = localRoutingId)
                        },
                        onFailure = { error ->
                            mutableRegisteredRoutingAliases.value = emptySet()
                            mutableConnectionState.value =
                                TransportConnectionState.Failed(
                                    message = error.message ?: "Presence route failed"
                                )
                            this@webSocket.close(
                                CloseReason(
                                    code = CloseReason.Codes.INTERNAL_ERROR,
                                    message = "Presence route failed"
                                )
                            )
                        }
                    )

                try {
                    consumeIncomingFrames(
                        expectedRoutingId = localRoutingId,
                        onGatewayRegistered = {
                            mutableRegisteredRoutingAliases.value = emptySet()
                            presenceSession.onGatewayRegistered()
                        },
                        onRouteRegistered = presenceSession::onRouteAccepted,
                        onRouteRejected = presenceSession::onRouteRejected
                    )
                } finally {
                    presenceSession.close()
                }

                logSessionEnd()
            }

            if (mutableConnectionState.value !is TransportConnectionState.Failed) {
                mutableConnectionState.value = TransportConnectionState.Disconnected
            }
        } catch (error: CancellationException) {
            mutableConnectionState.value = TransportConnectionState.Disconnected
            throw error
        } catch (error: Throwable) {
            logger.error(error) { "WebSocket connection failed" }
            mutableConnectionState.value =
                TransportConnectionState.Failed(
                    message = error.message ?: "WebSocket connection failed"
                )
        } finally {
            mutableRegisteredRoutingAliases.value = emptySet()
            sessionMutex.withLock {
                session = null
            }
            pendingRequestRegistry.failAll(
                IllegalStateException("WebSocket connection closed")
            )
            connectionJob = null
        }
    }

    private suspend fun DefaultClientWebSocketSession.consumeIncomingFrames(
        expectedRoutingId: String,
        onGatewayRegistered: () -> Unit,
        onRouteRegistered: (Set<String>) -> Unit,
        onRouteRejected: (Throwable) -> Unit
    ) {
        incoming.consumeEach { frame ->
            when (frame) {
                is Frame.Text ->
                    serverMessageHandler.handle(
                        encodedMessage = frame.readText(),
                        expectedRoutingId = expectedRoutingId,
                        onGatewayRegistered = onGatewayRegistered,
                        onRouteRegistered = onRouteRegistered,
                        onRouteRejected = onRouteRejected,
                        onIncomingEnvelope = mutableIncomingEnvelopes::emit,
                        onTypingEvent = mutableIncomingTypingEvents::emit
                    )

                is Frame.Close -> logCloseFrame()
                is Frame.Binary -> logger.warn { "Ignoring unsupported binary WebSocket frame" }
                is Frame.Ping,
                is Frame.Pong -> Unit
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.logCloseFrame() {
        val reason = closeReason.await()
        logger.info {
            "Received WebSocket close frame: code=${reason?.code}, message=${reason?.message}"
        }
    }

    private suspend fun DefaultClientWebSocketSession.logSessionEnd() {
        val reason = closeReason.await()
        logger.info {
            "WebSocket session ended: code=${reason?.code}, message=${reason?.message}"
        }
    }

    private suspend fun sendRegistration(
        localRoutingId: String,
        connectionId: String
    ) {
        sendClientMessage(
            GatewayClientMessage.Register(
                routingId = localRoutingId,
                connectionId = connectionId
            )
        )
        logger.debug {
            "Gateway registration sent for $localRoutingId; presenceDeferred=true"
        }
    }

    private suspend fun sendClientMessage(message: GatewayClientMessage) {
        sendMutex.withLock {
            val activeSession =
                sessionMutex.withLock { session }
                    ?: error("WebSocket session is not available")
            activeSession.send(
                Frame.Text(
                    json.encodeToString<GatewayClientMessage>(message)
                )
            )
        }
    }

    private suspend fun awaitEnvelopeAcceptance(
        envelopeId: String,
        timeoutMilliseconds: Long,
        send: suspend () -> Unit
    ): Result<GatewayEnvelopeAcceptance> {
        if (connectionState.value !is TransportConnectionState.Connected) {
            return Result.failure(IllegalStateException("WebSocket transport is not connected"))
        }
        return pendingRequestRegistry.awaitEnvelopeAcceptance(
            envelopeId = envelopeId,
            timeoutMilliseconds = timeoutMilliseconds,
            send = send
        )
    }

    private companion object {
        const val INCOMING_BUFFER_CAPACITY = 64
    }
}
