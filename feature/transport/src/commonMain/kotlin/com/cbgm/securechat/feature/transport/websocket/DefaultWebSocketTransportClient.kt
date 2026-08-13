package com.cbgm.securechat.feature.transport.websocket

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.feature.transport.gateway.model.ClientRouteRegistration
import com.cbgm.securechat.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.securechat.feature.transport.gateway.model.GatewayClientMessage
import com.cbgm.securechat.feature.transport.gateway.model.GatewayServerMessage
import com.cbgm.securechat.feature.transport.gateway.model.GatewayTypingEvent
import com.cbgm.securechat.feature.transport.gateway.model.TransportEnvelope
import com.cbgm.securechat.feature.transport.presence.ClientPresenceRouteManager
import com.cbgm.securechat.feature.transport.presence.PresenceRouteConnection
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

class DefaultWebSocketTransportClient internal constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val presenceRouteManager: ClientPresenceRouteManager
) : WebSocketTransportClient {
    private val logger = SecureChatLog.withTag("DefaultWebSocketTransportClient")

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

    private val acknowledgementsMutex = Mutex()

    private var session: DefaultClientWebSocketSession? = null

    private var connectionJob: Job? = null

    private val pendingAcknowledgements = mutableMapOf<String, CompletableDeferred<Unit>>()

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
        runCatching {
            require(timeoutMilliseconds > 0L) {
                "Acknowledgement timeout must be positive"
            }

            check(connectionState.value is TransportConnectionState.Connected) {
                "WebSocket transport is not connected"
            }

            val acknowledgement = CompletableDeferred<Unit>()

            acknowledgementsMutex.withLock {
                check(!pendingAcknowledgements.containsKey(envelope.envelopeId)) {
                    "Envelope is already awaiting acknowledgement"
                }

                pendingAcknowledgements[envelope.envelopeId] = acknowledgement
            }

            try {
                sendEnvelopeFrame(envelope = envelope)

                withTimeout(timeoutMilliseconds.milliseconds) {
                    acknowledgement.await()
                }
            } finally {
                acknowledgementsMutex.withLock {
                    pendingAcknowledgements.remove(envelope.envelopeId)
                }
            }
        }

    override suspend fun sendFederatedEnvelopeAndAwaitAcceptance(
        envelope: FederatedEnvelope,
        timeoutMilliseconds: Long
    ): Result<Unit> =
        awaitEnvelopeAcceptance(envelope.envelopeId, timeoutMilliseconds) {
            sendFederatedEnvelopeFrame(envelope)
        }

    override suspend fun awaitRoutingAlias(
        routingAlias: String,
        timeoutMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            require(routingAlias.isNotBlank()) { "Routing alias must not be blank" }
            require(timeoutMilliseconds > 0L) { "Routing alias timeout must be positive" }
            withTimeout(timeoutMilliseconds.milliseconds) {
                mutableRegisteredRoutingAliases.first { aliases -> routingAlias in aliases }
            }
        }

    override suspend fun acknowledgeIncomingEnvelope(envelopeId: String): Result<Unit> =
        runCatching {
            require(envelopeId.isNotBlank()) {
                "Envelope ID must not be blank"
            }

            sendMutex.withLock {
                val activeSession =
                    sessionMutex.withLock {
                        session
                    } ?: error(
                        "WebSocket session is not available"
                    )

                val clientMessage =
                    GatewayClientMessage.AcknowledgeEnvelope(
                        envelopeId = envelopeId
                    )

                val encodedMessage =
                    json.encodeToString<GatewayClientMessage>(
                        clientMessage
                    )

                activeSession.send(
                    Frame.Text(encodedMessage)
                )
            }
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

            sendMutex.withLock {
                val activeSession =
                    sessionMutex.withLock {
                        session
                    } ?: error(
                        "WebSocket session is not available"
                    )

                val clientMessage =
                    GatewayClientMessage.TypingState(
                        recipientId = recipientId,
                        isTyping = isTyping
                    )

                activeSession.send(
                    Frame.Text(
                        json.encodeToString<GatewayClientMessage>(
                            clientMessage
                        )
                    )
                )
            }
        }

    override suspend fun disconnect() {
        val activeConnectionJob = connectionJob

        connectionJob = null

        val activeSession =
            sessionMutex.withLock {
                val result = session
                session = null

                result
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

        failPendingAcknowledgements(error = IllegalStateException("WebSocket disconnected"))

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

                val gatewayInformation =
                    presenceRouteManager
                        .fetchGatewayInformation(serverUrl = serverUrl)
                        .onFailure { error ->
                            logger.warn {
                                "Gateway information unavailable; using compatible registration: " +
                                    (error.message ?: "unknown error")
                            }
                        }.getOrNull()

                val presenceConnection =
                    PresenceRouteConnection(
                        serverUrl = serverUrl,
                        routingId = localRoutingId,
                        connectionId = connectionId,
                        generation = generation,
                        initialGatewayInformation = gatewayInformation,
                        initialRouteEstablished = false,
                        connectionIdRegistered = gatewayInformation != null
                    )
                val routeRegistration =
                    gatewayInformation?.let { information ->
                        presenceRouteManager
                            .createRegistration(
                                connection = presenceConnection,
                                gatewayInformation = information
                            ).getOrElse { error ->
                                throw IllegalStateException(
                                    "Could not create signed presence route",
                                    error
                                )
                            }
                    }

                val signedRegistrationSent =
                    sendRegistration(
                        activeSession = this,
                        localRoutingId = localRoutingId,
                        connectionId = connectionId,
                        connectionIdRegistered = gatewayInformation != null,
                        routeRegistration = routeRegistration
                    )

                val routeRefreshJob =
                    launch {
                        presenceRouteManager.maintain(
                            connection =
                                presenceConnection.copy(
                                    initialRouteEstablished = signedRegistrationSent
                                ),
                            sendRefresh = { registration ->
                                runCatching {
                                    sendMutex.withLock {
                                        this@webSocket.send(
                                            Frame.Text(
                                                json.encodeToString<GatewayClientMessage>(
                                                    GatewayClientMessage.RefreshRoute(registration)
                                                )
                                            )
                                        )
                                    }
                                    mutableRegisteredRoutingAliases.value =
                                        registration.route.aliases.orEmpty().toSet()
                                }
                            },
                            reconnect = {
                                this@webSocket.close(
                                    CloseReason(
                                        code = CloseReason.Codes.NORMAL,
                                        message = "Reconnect for signed presence"
                                    )
                                )
                            },
                            fail = { error ->
                                mutableConnectionState.value =
                                    TransportConnectionState.Failed(
                                        message = error?.message ?: "Presence route refresh failed"
                                    )
                                this@webSocket.close(
                                    CloseReason(
                                        code = CloseReason.Codes.INTERNAL_ERROR,
                                        message = "Presence route refresh failed"
                                    )
                                )
                            }
                        )
                    }

                try {
                    incoming.consumeEach { frame ->
                        when (frame) {
                            is Frame.Text -> {
                                handleTextFrame(
                                    encodedMessage = frame.readText(),
                                    expectedRoutingId = localRoutingId,
                                    registeredAliases =
                                        routeRegistration
                                            ?.route
                                            ?.aliases
                                            .orEmpty()
                                            .toSet()
                                )
                            }

                            is Frame.Close -> {
                                val reason = closeReason.await()

                                logger.info {
                                    "Received WebSocket close frame: " +
                                        "code=${reason?.code}, " +
                                        "message=${reason?.message}"
                                }
                            }

                            is Frame.Binary -> {
                                logger.warn { "Ignoring unsupported binary WebSocket frame" }
                            }

                            is Frame.Ping -> {
                                /*
                                 * Ktor handles pong responses internally.
                                 */
                            }

                            is Frame.Pong -> {
                                /*
                                 * Ktor handles ping/pong internally.
                                 */
                            }
                        }
                    }
                } finally {
                    routeRefreshJob.cancelAndJoin()
                }

                val reason = closeReason.await()

                logger.info {
                    "WebSocket session ended: " +
                        "code=${reason?.code}, " +
                        "message=${reason?.message}"
                }
            }

            if (mutableConnectionState.value !is TransportConnectionState.Failed) {
                mutableConnectionState.value = TransportConnectionState.Disconnected
            }
        } catch (
            error: CancellationException
        ) {
            mutableConnectionState.value = TransportConnectionState.Disconnected

            throw error
        } catch (
            error: Throwable
        ) {
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

            failPendingAcknowledgements(error = IllegalStateException("WebSocket connection closed"))

            connectionJob = null
        }
    }

    private suspend fun sendRegistration(
        activeSession: DefaultClientWebSocketSession,
        localRoutingId: String,
        connectionId: String,
        connectionIdRegistered: Boolean,
        routeRegistration: ClientRouteRegistration?
    ): Boolean {
        val registration =
            GatewayClientMessage.Register(
                routingId = localRoutingId,
                connectionId = connectionId.takeIf { connectionIdRegistered },
                generation = routeRegistration?.route?.generation,
                expiresAtEpochMilliseconds = routeRegistration?.route?.expiresAtEpochMilliseconds,
                aliases = routeRegistration?.route?.aliases?.takeIf { it.isNotEmpty() },
                clientSigningPublicKey = routeRegistration?.clientSigningPublicKey,
                clientSignature = routeRegistration?.route?.clientSignature
            )

        val encodedRegistration = json.encodeToString<GatewayClientMessage>(registration)

        sendMutex.withLock {
            activeSession.send(Frame.Text(encodedRegistration))
        }

        logger.debug {
            "Gateway registration sent for $localRoutingId; signed=${routeRegistration != null}"
        }

        return routeRegistration != null
    }

    private suspend fun sendEnvelopeFrame(envelope: TransportEnvelope) {
        sendMutex.withLock {
            val activeSession =
                sessionMutex.withLock {
                    session
                } ?: error(
                    "WebSocket session is not available"
                )

            val clientMessage = GatewayClientMessage.SendEnvelope(envelope = envelope)

            val encodedMessage = json.encodeToString<GatewayClientMessage>(clientMessage)

            activeSession.send(Frame.Text(encodedMessage))
        }
    }

    private suspend fun sendFederatedEnvelopeFrame(envelope: FederatedEnvelope) {
        sendMutex.withLock {
            val activeSession =
                sessionMutex.withLock { session }
                    ?: error("WebSocket session is not available")
            activeSession.send(
                Frame.Text(
                    json.encodeToString<GatewayClientMessage>(
                        GatewayClientMessage.SendFederatedEnvelope(envelope)
                    )
                )
            )
        }
    }

    private suspend fun awaitEnvelopeAcceptance(
        envelopeId: String,
        timeoutMilliseconds: Long,
        send: suspend () -> Unit
    ): Result<Unit> =
        runCatching {
            require(timeoutMilliseconds > 0L) { "Acknowledgement timeout must be positive" }
            check(connectionState.value is TransportConnectionState.Connected) {
                "WebSocket transport is not connected"
            }
            val acknowledgement = CompletableDeferred<Unit>()
            acknowledgementsMutex.withLock {
                check(!pendingAcknowledgements.containsKey(envelopeId)) {
                    "Envelope is already awaiting acknowledgement"
                }
                pendingAcknowledgements[envelopeId] = acknowledgement
            }
            try {
                send()
                withTimeout(timeoutMilliseconds.milliseconds) { acknowledgement.await() }
            } finally {
                acknowledgementsMutex.withLock { pendingAcknowledgements.remove(envelopeId) }
            }
        }

    private suspend fun handleTextFrame(
        encodedMessage: String,
        expectedRoutingId: String,
        registeredAliases: Set<String>
    ) {
        val message =
            runCatching {
                json.decodeFromString<GatewayServerMessage>(encodedMessage)
            }.getOrElse { error ->
                logger.error(error) { "Invalid gateway response" }

                mutableConnectionState.value =
                    TransportConnectionState.Failed(
                        message = error.message ?: "Invalid gateway response"
                    )

                return
            }

        when (message) {
            is GatewayServerMessage.Registered -> {
                if (message.routingId != expectedRoutingId) {
                    mutableConnectionState.value =
                        TransportConnectionState.Failed(message = "Gateway registered an unexpected routing identity")

                    return
                }

                logger.info { "Gateway registration accepted for ${message.routingId}" }

                mutableRegisteredRoutingAliases.value = registeredAliases
                mutableConnectionState.value =
                    TransportConnectionState.Connected(routingId = message.routingId)
            }

            is GatewayServerMessage.IncomingEnvelope -> {
                mutableIncomingEnvelopes.emit(message.envelope)
            }

            is GatewayServerMessage.TypingState -> {
                mutableIncomingTypingEvents.emit(
                    GatewayTypingEvent(
                        senderId = message.senderId,
                        isTyping = message.isTyping
                    )
                )
            }

            is GatewayServerMessage.EnvelopeAccepted -> {
                val acknowledgement =
                    acknowledgementsMutex.withLock {
                        pendingAcknowledgements[message.envelopeId]
                    }

                acknowledgement?.complete(Unit)
            }

            is GatewayServerMessage.Error -> {
                logger.warn { "Gateway error ${message.code}: ${message.message}" }

                if (message.code in FATAL_ROUTE_ERROR_CODES) {
                    throw IllegalStateException(
                        "Presence route rejected by gateway: ${message.code}"
                    )
                }

                /*
                 * A gateway error does not always mean the underlying
                 * WebSocket connection is broken.
                 *
                 * Keep the connection state unchanged here. The
                 * envelope awaiting acknowledgement will eventually
                 * time out and the outbox item becomes FAILED.
                 */
            }
        }
    }

    private suspend fun failPendingAcknowledgements(error: Throwable) {
        val acknowledgements =
            acknowledgementsMutex.withLock {
                val values = pendingAcknowledgements.values.toList()

                pendingAcknowledgements.clear()

                values
            }

        acknowledgements.forEach { acknowledgement ->

            acknowledgement.completeExceptionally(error)
        }
    }

    private companion object {
        const val INCOMING_BUFFER_CAPACITY = 64

        val FATAL_ROUTE_ERROR_CODES =
            setOf(
                "INVALID_ROUTE",
                "INVALID_ROUTE_REFRESH",
                "ROUTE_REJECTED",
                "ALREADY_REGISTERED"
            )
    }
}
