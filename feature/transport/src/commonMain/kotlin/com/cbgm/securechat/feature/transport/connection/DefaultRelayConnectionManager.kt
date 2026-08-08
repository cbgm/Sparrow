package com.cbgm.securechat.feature.transport.connection

import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.core.transport.TransportDiagnostics
import com.cbgm.securechat.core.transport.TransportDiagnosticsProvider
import com.cbgm.securechat.feature.transport.discovery.FailedNodeTracker
import com.cbgm.securechat.feature.transport.discovery.NodeEndpoint
import com.cbgm.securechat.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import com.cbgm.securechat.feature.transport.relay.identity.LocalRelayIdProvider
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

class DefaultRelayConnectionManager(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val localRelayIdProvider: LocalRelayIdProvider,
    private val relayTransportConfig: RelayTransportConfig,
    private val nodeEndpointResolver: NodeEndpointResolver
) : RelayConnectionManager,
    TransportDiagnosticsProvider {
    private val logger = SecureChatLog.withTag("DefaultRelayConnectionManager")
    private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var connectionLoopJob: Job? = null
    private var resolvedEndpoints: List<NodeEndpoint> = emptyList()

    private val failedNodeTracker =
        FailedNodeTracker(
            cooldownMilliseconds = relayTransportConfig.failedNodeCooldownMilliseconds,
            now = SystemClock::nowEpochMilliseconds
        )

    private val diagnosticsState =
        RelayTransportDiagnosticsState(
            registryUrl = relayTransportConfig.nodeRegistryBaseUrl
        )

    override val diagnostics: StateFlow<TransportDiagnostics> = diagnosticsState.diagnostics

    override val connectionState: StateFlow<TransportConnectionState> =
        webSocketTransportClient.connectionState

    override fun start() {
        if (connectionLoopJob?.isActive == true) {
            return
        }

        connectionLoopJob = connectionScope.launch { runConnectionLoop() }
    }

    override suspend fun stop() {
        val activeJob = connectionLoopJob

        connectionLoopJob = null
        activeJob?.cancelAndJoin()
        webSocketTransportClient.disconnect()
        diagnosticsState.stopped(
            unavailableNodeIds = failedNodeTracker.unavailableNodeIds(resolvedEndpoints)
        )
    }

    private suspend fun runConnectionLoop() {
        var reconnectDelay = INITIAL_RECONNECT_DELAY_MILLISECONDS

        while (connectionScope.isActive) {
            val connected = runConnectionAttempt()

            runCatching {
                webSocketTransportClient.disconnect()
            }

            if (connected) {
                reconnectDelay = INITIAL_RECONNECT_DELAY_MILLISECONDS
            }

            logger.debug { "Relay reconnecting in ${reconnectDelay}ms" }
            delay(reconnectDelay.milliseconds)

            reconnectDelay =
                (reconnectDelay * 2L)
                    .coerceAtMost(MAX_RECONNECT_DELAY_MILLISECONDS)
        }
    }

    private suspend fun runConnectionAttempt(): Boolean {
        var selectedEndpoint: NodeEndpoint? = null

        return try {
            diagnosticsState.connecting()

            val relayId = localRelayIdProvider.getLocalRelayId().getOrThrow()
            val endpoint = resolveEndpoint(relayId)
            selectedEndpoint = endpoint

            connect(endpoint = endpoint, relayId = relayId)

            when (val connectionResult = awaitConnectionResult()) {
                is TransportConnectionState.Connected -> {
                    onConnected(endpoint = endpoint, relayId = connectionResult.relayId)
                    val endState = waitForConnectionEnd(relayId)
                    onConnectedNodeEnded(endpoint = endpoint, state = endState)
                    true
                }

                is TransportConnectionState.Failed -> {
                    onConnectionFailure(endpoint = endpoint, message = connectionResult.message)
                    false
                }

                else -> false
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            logger.error(error) { "Relay connection error" }
            selectedEndpoint?.let { endpoint ->
                failedNodeTracker.recordFailure(endpoint.nodeId)
            }
            diagnosticsState.failed(
                endpoint = selectedEndpoint,
                message = error.message ?: "Relay connection error",
                unavailableNodeIds = failedNodeTracker.unavailableNodeIds(resolvedEndpoints)
            )
            false
        }
    }

    private suspend fun resolveEndpoint(relayId: String): NodeEndpoint {
        val endpoints = nodeEndpointResolver.resolve(relayId).getOrThrow()
        resolvedEndpoints = endpoints
        val availableEndpoints = failedNodeTracker.available(endpoints)
        val unavailableNodeIds = failedNodeTracker.unavailableNodeIds(endpoints)

        diagnosticsState.resolved(
            endpoints = endpoints,
            unavailableNodeIds = unavailableNodeIds,
            registryAuthorityVerified = relayTransportConfig.nodeRegistryBaseUrl != null
        )

        return checkNotNull(availableEndpoints.firstOrNull()) {
            "Every discovered relay node is temporarily unavailable"
        }
    }

    private fun connect(
        endpoint: NodeEndpoint,
        relayId: String
    ) {
        diagnosticsState.selected(
            endpoint = endpoint,
            unavailableNodeIds = failedNodeTracker.unavailableNodeIds(resolvedEndpoints)
        )
        logger.debug {
            "Connecting to node ${endpoint.nodeId} as $relayId"
        }
        webSocketTransportClient.connect(
            serverUrl = endpoint.websocketUrl,
            localRelayId = relayId
        )
    }

    private suspend fun awaitConnectionResult(): TransportConnectionState =
        withTimeout(CONNECTION_TIMEOUT_MILLISECONDS.milliseconds) {
            webSocketTransportClient
                .connectionState
                .first { state ->
                    state is TransportConnectionState.Connected ||
                        state is TransportConnectionState.Failed
                }
        }

    private fun onConnected(
        endpoint: NodeEndpoint,
        relayId: String
    ) {
        failedNodeTracker.recordSuccess(endpoint.nodeId)
        diagnosticsState.connected(
            endpoint = endpoint,
            unavailableNodeIds = failedNodeTracker.unavailableNodeIds(resolvedEndpoints)
        )
        logger.info {
            "Relay connected through node ${endpoint.nodeId} as $relayId"
        }
    }

    private fun onConnectedNodeEnded(
        endpoint: NodeEndpoint,
        state: TransportConnectionState
    ) {
        failedNodeTracker.recordFailure(endpoint.nodeId)
        diagnosticsState.connectedNodeEnded(
            endpoint = endpoint,
            state = state,
            unavailableNodeIds = failedNodeTracker.unavailableNodeIds(resolvedEndpoints)
        )
    }

    private fun onConnectionFailure(
        endpoint: NodeEndpoint,
        message: String
    ) {
        failedNodeTracker.recordFailure(endpoint.nodeId)
        logger.warn { "Relay connection failed: $message" }
        diagnosticsState.failed(
            endpoint = endpoint,
            message = message,
            unavailableNodeIds = failedNodeTracker.unavailableNodeIds(resolvedEndpoints)
        )
    }

    private suspend fun waitForConnectionEnd(relayId: String): TransportConnectionState =
        coroutineScope {
            val refreshJob =
                launch {
                    while (isActive) {
                        delay(relayTransportConfig.directoryRefreshIntervalMilliseconds.milliseconds)
                        nodeEndpointResolver.resolve(relayId).fold(
                            onSuccess = { endpoints ->
                                resolvedEndpoints = endpoints
                                diagnosticsState.resolved(
                                    endpoints = endpoints,
                                    unavailableNodeIds =
                                        failedNodeTracker.unavailableNodeIds(endpoints),
                                    registryAuthorityVerified =
                                        relayTransportConfig.nodeRegistryBaseUrl != null
                                )
                            },
                            onFailure = { error ->
                                logger.warn {
                                    "Signed node directory refresh failed: " +
                                        (error.message ?: "unknown error")
                                }
                            }
                        )
                    }
                }

            try {
                webSocketTransportClient.connectionState.first { state ->
                    state is TransportConnectionState.Disconnected ||
                        state is TransportConnectionState.Failed
                }
            } finally {
                refreshJob.cancelAndJoin()
            }
        }

    private companion object {
        const val CONNECTION_TIMEOUT_MILLISECONDS = 15_000L
        const val INITIAL_RECONNECT_DELAY_MILLISECONDS = 1_000L
        const val MAX_RECONNECT_DELAY_MILLISECONDS = 30_000L
    }
}
