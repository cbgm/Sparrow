package com.cbgm.sparrow.feature.transport.connection

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.TransportDiagnostics
import com.cbgm.sparrow.core.transport.TransportDiagnosticsProvider
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.controlplane.NodeControlPlaneDiscoverySynchronizer
import com.cbgm.sparrow.feature.transport.discovery.FailedNodeTracker
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpoint
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
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

class DefaultTransportConnectionManager(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val transportConfig: TransportConfig,
    private val nodeEndpointResolver: NodeEndpointResolver,
    private val controlPlaneConfiguration: ControlPlaneConfiguration,
    private val controlPlaneDiscoverySynchronizer: NodeControlPlaneDiscoverySynchronizer
) : TransportConnectionManager,
    TransportDiagnosticsProvider {
    private val logger = SparrowLog.withTag("DefaultTransportConnectionManager")
    private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var connectionLoopJob: Job? = null
    private var resolvedEndpoints: List<NodeEndpoint> = emptyList()

    private val failedNodeTracker =
        FailedNodeTracker(
            cooldownMilliseconds = transportConfig.failedNodeCooldownMilliseconds,
            now = SystemClock::nowEpochMilliseconds
        )

    private val diagnosticsState =
        TransportDiagnosticsState(
            registryUrl = controlPlaneConfiguration.activeEndpoint.value?.baseUrl,
            missingNodeCooldownMilliseconds = transportConfig.failedNodeCooldownMilliseconds,
            now = SystemClock::nowEpochMilliseconds
        )

    override val diagnostics: StateFlow<TransportDiagnostics> = diagnosticsState.diagnostics

    override val refreshDiagnostics: suspend () -> Unit = {
        localRoutingIdProvider.getLocalRoutingId().getOrNull()?.let { routingId ->
            nodeEndpointResolver
                .resolve(
                    localRoutingId = routingId,
                    forceRefresh = true
                ).fold(
                    onSuccess = { endpoints ->
                        resolvedEndpoints = endpoints
                        diagnosticsState.resolved(
                            endpoints = endpoints,
                            cooldownUntilEpochMillisecondsByNodeId =
                                failedNodeTracker.cooldownUntilEpochMillisecondsByNodeId(endpoints),
                            registryAuthorityVerified = true,
                            registryUrl = controlPlaneConfiguration.activeEndpoint.value?.baseUrl
                        )
                    },
                    onFailure = { error ->
                        logger.warn {
                            "Live transport diagnostics refresh failed: " +
                                (error.message ?: "unknown error")
                        }
                    }
                )
        }
    }

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
            cooldownUntilEpochMillisecondsByNodeId =
                failedNodeTracker.cooldownUntilEpochMillisecondsByNodeId(resolvedEndpoints)
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

            logger.debug { "Transport reconnecting in ${reconnectDelay}ms" }
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

            val routingId = localRoutingIdProvider.getLocalRoutingId().getOrThrow()
            val endpoint = resolveEndpoint(routingId)
            selectedEndpoint = endpoint

            connect(endpoint = endpoint, routingId = routingId)

            when (val connectionResult = awaitConnectionResult()) {
                is TransportConnectionState.Connected -> {
                    onConnected(endpoint = endpoint, routingId = connectionResult.routingId)
                    val endState = waitForConnectionEnd(routingId, endpoint)
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
            logger.error(error) { "Transport connection error" }
            selectedEndpoint?.let { endpoint ->
                failedNodeTracker.recordFailure(endpoint.nodeId)
            }
            diagnosticsState.failed(
                endpoint = selectedEndpoint,
                message = error.message ?: "Transport connection error",
                cooldownUntilEpochMillisecondsByNodeId =
                    failedNodeTracker.cooldownUntilEpochMillisecondsByNodeId(resolvedEndpoints)
            )
            false
        }
    }

    private suspend fun resolveEndpoint(routingId: String): NodeEndpoint {
        var endpoints = nodeEndpointResolver.resolve(routingId).getOrThrow()
        var availableEndpoints = failedNodeTracker.available(endpoints)

        if (availableEndpoints.isEmpty()) {
            nodeEndpointResolver
                .resolve(
                    localRoutingId = routingId,
                    forceRefresh = true
                ).onSuccess { refreshedEndpoints ->
                    endpoints = refreshedEndpoints
                    availableEndpoints = failedNodeTracker.available(refreshedEndpoints)
                }.onFailure { error ->
                    logger.warn {
                        "Node directory refresh before cooldown probe failed: " +
                            (error.message ?: "unknown error")
                    }
                }
        }

        resolvedEndpoints = endpoints
        diagnosticsState.resolved(
            endpoints = endpoints,
            cooldownUntilEpochMillisecondsByNodeId =
                failedNodeTracker.cooldownUntilEpochMillisecondsByNodeId(endpoints),
            registryAuthorityVerified = true,
            registryUrl = controlPlaneConfiguration.activeEndpoint.value?.baseUrl
        )

        availableEndpoints.firstOrNull()?.let { return it }

        return checkNotNull(failedNodeTracker.probeCandidate(endpoints)) {
            "Node directory does not contain a gateway node to probe"
        }.also { endpoint ->
            logger.warn {
                "Every discovered gateway node is cooling down; probing ${endpoint.nodeId}"
            }
        }
    }

    private fun connect(
        endpoint: NodeEndpoint,
        routingId: String
    ) {
        diagnosticsState.selected(
            endpoint = endpoint,
            cooldownUntilEpochMillisecondsByNodeId =
                failedNodeTracker.cooldownUntilEpochMillisecondsByNodeId(resolvedEndpoints)
        )
        logger.debug {
            "Connecting to node ${endpoint.nodeId} as $routingId"
        }
        webSocketTransportClient.connect(
            serverUrl = endpoint.websocketUrl,
            localRoutingId = routingId
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

    private suspend fun onConnected(
        endpoint: NodeEndpoint,
        routingId: String
    ) {
        failedNodeTracker.recordSuccess(endpoint.nodeId)
        diagnosticsState.connected(
            endpoint = endpoint,
            cooldownUntilEpochMillisecondsByNodeId =
                failedNodeTracker.cooldownUntilEpochMillisecondsByNodeId(resolvedEndpoints)
        )
        logger.info {
            "Transport connected through node ${endpoint.nodeId} as $routingId"
        }
        controlPlaneDiscoverySynchronizer
            .refreshFromNode(endpoint.websocketUrl)
            .onSuccess { count ->
                logger.info { "Control-plane discovery synchronized $count trusted addresses" }
            }.onFailure { error ->
                logger.warn {
                    "Control-plane discovery failed: ${error.message ?: "unknown error"}"
                }
            }
    }

    private fun onConnectedNodeEnded(
        endpoint: NodeEndpoint,
        state: TransportConnectionState
    ) {
        if (state is TransportConnectionState.Failed) {
            failedNodeTracker.recordFailure(endpoint.nodeId)
        } else {
            logger.info {
                "Transport session on ${endpoint.nodeId} ended without a transport failure; " +
                    "retrying without node cooldown"
            }
        }
        diagnosticsState.connectedNodeEnded(
            endpoint = endpoint,
            state = state,
            cooldownUntilEpochMillisecondsByNodeId =
                failedNodeTracker.cooldownUntilEpochMillisecondsByNodeId(resolvedEndpoints)
        )
    }

    private fun onConnectionFailure(
        endpoint: NodeEndpoint,
        message: String
    ) {
        failedNodeTracker.recordFailure(endpoint.nodeId)
        logger.warn { "Transport connection failed: $message" }
        diagnosticsState.failed(
            endpoint = endpoint,
            message = message,
            cooldownUntilEpochMillisecondsByNodeId =
                failedNodeTracker.cooldownUntilEpochMillisecondsByNodeId(resolvedEndpoints)
        )
    }

    private suspend fun waitForConnectionEnd(
        routingId: String,
        endpoint: NodeEndpoint
    ): TransportConnectionState =
        coroutineScope {
            val refreshJob =
                launch {
                    while (isActive) {
                        delay(transportConfig.directoryRefreshIntervalMilliseconds.milliseconds)
                        nodeEndpointResolver.resolve(routingId).fold(
                            onSuccess = { endpoints ->
                                resolvedEndpoints = endpoints
                                diagnosticsState.resolved(
                                    endpoints = endpoints,
                                    cooldownUntilEpochMillisecondsByNodeId =
                                        failedNodeTracker
                                            .cooldownUntilEpochMillisecondsByNodeId(endpoints),
                                    registryAuthorityVerified = true,
                                    registryUrl = controlPlaneConfiguration.activeEndpoint.value?.baseUrl
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
            val controlPlaneDiscoveryJob =
                launch {
                    while (isActive) {
                        delay(CONTROL_PLANE_DISCOVERY_REFRESH_MILLISECONDS.milliseconds)
                        controlPlaneDiscoverySynchronizer
                            .refreshFromNode(endpoint.websocketUrl)
                            .onFailure { error ->
                                logger.warn {
                                    "Control-plane discovery refresh failed: " +
                                        (error.message ?: "unknown error")
                                }
                            }
                    }
                }

            try {
                webSocketTransportClient.connectionState.first { state ->
                    state is TransportConnectionState.Disconnected ||
                        state is TransportConnectionState.Failed
                }
            } finally {
                refreshJob.cancelAndJoin()
                controlPlaneDiscoveryJob.cancelAndJoin()
            }
        }

    private companion object {
        const val CONNECTION_TIMEOUT_MILLISECONDS = 15_000L
        const val INITIAL_RECONNECT_DELAY_MILLISECONDS = 1_000L
        const val MAX_RECONNECT_DELAY_MILLISECONDS = 30_000L
        const val CONTROL_PLANE_DISCOVERY_REFRESH_MILLISECONDS = 300_000L
    }
}
