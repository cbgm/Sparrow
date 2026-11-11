package com.cbgm.sparrow.feature.transport.connection

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.feature.transport.TransportDiagnostics
import com.cbgm.sparrow.feature.transport.TransportDiagnosticsProvider
import com.cbgm.sparrow.feature.transport.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.circuit.CircuitStatus
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.controlplane.NodeControlPlaneDiscoverySynchronizer
import com.cbgm.sparrow.feature.transport.discovery.FailedNodeTracker
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpoint
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.discovery.PreferredNodeReturnPolicy
import com.cbgm.sparrow.feature.transport.discovery.PreferredNodeStore
import com.cbgm.sparrow.feature.transport.presence.AliasClaimRegistrar
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds

/**
 * Keeps the app reachable without a session at any node: it picks the home node (the preferred one when it is in the
 * signed node directory, otherwise the first available), which is the first hop of the circuits and answers link
 * previews and exchange rates, and counts as connected while a circuit is up. It keeps the node directory and the
 * Control Plane discovery fresh, and claims the phone alias with the invitation mailbox at start and every
 * [ALIAS_CLAIM_INTERVAL_MILLISECONDS]. Messages go through mailboxes; nothing arrives here.
 */
class DefaultTransportConnectionManager internal constructor(
    private val circuits: CircuitRunner,
    private val aliasClaims: AliasClaimRegistrar,
    private val localRoutingIdProvider: LocalRoutingIdProvider,
    private val transportConfig: TransportConfig,
    private val nodeEndpointResolver: NodeEndpointResolver,
    private val controlPlaneConfiguration: ControlPlaneConfiguration,
    private val controlPlaneDiscoverySynchronizer: NodeControlPlaneDiscoverySynchronizer,
    private val preferredNodeStore: PreferredNodeStore? = null
) : TransportConnectionManager,
    TransportDiagnosticsProvider {
    private val logger = SparrowLog.withTag("DefaultTransportConnectionManager")

    // IO, not Default: the many CPU-heavy startup tasks share Default and would delay connecting.
    private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectionLoopJob: Job? = null
    private var aliasClaimJob: Job? = null
    private var directoryRefreshJob: Job? = null

    /** Ends the time at the current home node, so the loop picks the node again (the preferred one first). */
    private val reselect = Channel<Unit>(Channel.CONFLATED)
    private var resolvedEndpoints: List<NodeEndpoint> = emptyList()

    @Volatile
    override var connectedNodeId: String? = null
        private set

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
        localRoutingIdProvider.getLocalRoutingId()
            .onFailure { failure -> logger.error(failure) { "Could not obtain local routing ID for diagnostics" } }
            .getOrNull()?.let { routingId ->
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
                            if (error is CancellationException) throw error
                            if (error.isRecoverableConnectivityFailure() || error.isUnavailableNodeDirectory()) {
                                logger.debug { "Transport diagnostics unavailable while offline: ${error.message}" }
                            } else {
                                logger.error(error) { "Live transport diagnostics refresh failed" }
                            }
                        }
                    )
            }
    }

    private val mutableConnectionState =
        MutableStateFlow<TransportConnectionState>(TransportConnectionState.Disconnected)

    override val connectionState: StateFlow<TransportConnectionState> =
        mutableConnectionState.asStateFlow()

    override fun start() {
        if (connectionLoopJob?.isActive == true) {
            return
        }

        connectionLoopJob = connectionScope.launch { runConnectionLoop() }
        aliasClaimJob = connectionScope.launch { runAliasClaims() }
    }

    /**
     * Phone aliases go to the Control Planes through the relays. They are claimed at start and again every
     * [ALIAS_CLAIM_INTERVAL_MILLISECONDS], which renews their binding and the invitation mailbox; a claim no Control
     * Plane answered is repeated after [ALIAS_CLAIM_RETRY_MILLISECONDS].
     */
    private suspend fun runAliasClaims() {
        while (connectionScope.isActive) {
            val bound =
                runCatching { aliasClaims.claim(localRoutingIdProvider.getLocalRoutingId().getOrThrow()) }
                    .onFailure { failure ->
                        if (failure is CancellationException) throw failure
                        logger.warn { "Phone alias claim failed: ${failure::class.simpleName}" }
                    }.getOrDefault(emptySet())
            delay((if (bound.isEmpty()) ALIAS_CLAIM_RETRY_MILLISECONDS else ALIAS_CLAIM_INTERVAL_MILLISECONDS).milliseconds)
        }
    }

    override suspend fun stop() {
        val activeJob = connectionLoopJob

        connectionLoopJob = null
        directoryRefreshJob?.cancel()
        aliasClaimJob?.cancelAndJoin()
        activeJob?.cancelAndJoin()
        connectedNodeId = null
        mutableConnectionState.value = TransportConnectionState.Disconnected
        diagnosticsState.stopped(
            cooldownUntilEpochMillisecondsByNodeId =
                failedNodeTracker.cooldownUntilEpochMillisecondsByNodeId(resolvedEndpoints)
        )
    }

    override suspend fun moveTo(nodeId: String) {
        val connectedElsewhere = connectedNodeId?.let { connected -> connected != nodeId } ?: false
        if (connectionLoopJob?.isActive == true && connectedElsewhere) {
            logger.info { "Moving to the preferred node $nodeId" }
            reselect.trySend(Unit)
        }
    }

    // Leaves a fallback node so the loop picks the node again, favourite first.
    private suspend fun returnToPreferredNode(
        connected: NodeEndpoint,
        endpoints: List<NodeEndpoint>
    ) {
        val preferredNodeId = preferredNodeStore?.get()
        val shouldReturn =
            PreferredNodeReturnPolicy.shouldReturn(
                preferredNodeId = preferredNodeId,
                connectedNodeId = connected.nodeId,
                endpoints = endpoints,
                coolingDownNodeIds = failedNodeTracker.unavailableNodeIds(endpoints)
            )
        if (shouldReturn) {
            logger.info { "Preferred node $preferredNodeId is back; moving to it" }
            reselect.trySend(Unit)
        }
    }

    private suspend fun runConnectionLoop() {
        var reconnectDelay = INITIAL_RECONNECT_DELAY_MILLISECONDS
        // Connect from the cached directory first; after a failed attempt wait for a fresh one,
        // so a removed or changed node is not retried from stale data.
        var useCachedDirectory = true

        while (connectionScope.isActive) {
            val connected = runConnectionAttempt(useCachedDirectory)
            useCachedDirectory = connected

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

    private suspend fun runConnectionAttempt(useCachedDirectory: Boolean): Boolean {
        var selectedEndpoint: NodeEndpoint? = null

        return try {
            mutableConnectionState.value = TransportConnectionState.Connecting
            diagnosticsState.connecting()

            val routingId = localRoutingIdProvider.getLocalRoutingId().getOrThrow()
            val endpoint = resolveEndpoint(routingId, useCachedDirectory)
            selectedEndpoint = endpoint

            select(endpoint = endpoint, routingId = routingId)

            when (val connectionResult = awaitConnectionResult(routingId)) {
                is TransportConnectionState.Connected -> {
                    mutableConnectionState.value = connectionResult
                    onConnected(endpoint = endpoint, routingId = connectionResult.routingId)
                    val endState = waitForConnectionEnd(routingId, endpoint)
                    onConnectedNodeEnded(endpoint = endpoint, state = endState)
                    true
                }

                is TransportConnectionState.Failed -> {
                    connectedNodeId = null
                    mutableConnectionState.value = connectionResult
                    onConnectionFailure(endpoint = endpoint, message = connectionResult.message)
                    false
                }

                else -> false
            }
        } catch (error: Throwable) {
            // The connection-attempt timeout is an offline/retry condition, not
            // cancellation of the foreground session. Preserve real job cancellation.
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            val failure =
                TransportConnectionState.Failed(
                    message = error.message ?: "Transport connection error"
                )
            connectedNodeId = null
            mutableConnectionState.value = failure
            if (error.isRecoverableConnectivityFailure() || error.isUnavailableNodeDirectory()) {
                logger.debug { "Transport unavailable; retrying: ${error.message ?: error::class.simpleName}" }
            } else {
                logger.error(error) { "Transport connection error" }
            }
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

    // With a usable cache the connection starts right away and the directory is refreshed in
    // parallel; otherwise the fresh directory is fetched before connecting.
    private suspend fun resolveEndpoint(
        routingId: String,
        useCachedDirectory: Boolean
    ): NodeEndpoint {
        val cachedEndpoints =
            if (useCachedDirectory) nodeEndpointResolver.resolveCached(routingId).getOrNull() else null
        val endpoints =
            if (cachedEndpoints != null) {
                refreshDirectoryInBackground(routingId)
                cachedEndpoints
            } else {
                // resolveCached already rejected the cache, so skip checking it again and fetch.
                nodeEndpointResolver.resolve(routingId, forceRefresh = useCachedDirectory).getOrThrow()
            }
        val availableEndpoints = failedNodeTracker.available(endpoints)

        publishResolvedEndpoints(endpoints)

        availableEndpoints.firstOrNull()?.let { return it }

        return checkNotNull(failedNodeTracker.probeCandidate(endpoints)) {
            "Node directory does not contain a gateway node to probe"
        }.also { endpoint ->
            logger.warn {
                "Every discovered gateway node is cooling down; probing ${endpoint.nodeId}"
            }
        }
    }

    // Fetches the fresh directory without blocking the connection and updates the known nodes.
    // The fetched directory is cached, so a retry after a failed attempt already uses it.
    private fun refreshDirectoryInBackground(routingId: String) {
        if (directoryRefreshJob?.isActive == true) return
        directoryRefreshJob =
            connectionScope.launch {
                nodeEndpointResolver.resolve(routingId).fold(
                    onSuccess = ::publishResolvedEndpoints,
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        logger.debug { "Node directory refresh failed: ${error.message}" }
                    }
                )
            }
    }

    private fun publishResolvedEndpoints(endpoints: List<NodeEndpoint>) {
        resolvedEndpoints = endpoints
        diagnosticsState.resolved(
            endpoints = endpoints,
            cooldownUntilEpochMillisecondsByNodeId =
                failedNodeTracker.cooldownUntilEpochMillisecondsByNodeId(endpoints),
            registryAuthorityVerified = true,
            registryUrl = controlPlaneConfiguration.activeEndpoint.value?.baseUrl
        )
    }

    /** The circuits start at the home node from now on ([connectedNodeId] is their first hop). */
    private fun select(
        endpoint: NodeEndpoint,
        routingId: String
    ) {
        connectedNodeId = endpoint.nodeId
        diagnosticsState.selected(
            endpoint = endpoint,
            cooldownUntilEpochMillisecondsByNodeId =
                failedNodeTracker.cooldownUntilEpochMillisecondsByNodeId(resolvedEndpoints)
        )
        logger.debug { "Home node ${endpoint.nodeId} for $routingId" }
    }

    /** Connected once a circuit is up; failed when none came up within [CONNECTION_TIMEOUT_MILLISECONDS]. */
    private suspend fun awaitConnectionResult(routingId: String): TransportConnectionState =
        withTimeoutOrNull(CONNECTION_TIMEOUT_MILLISECONDS.milliseconds) { circuits.status.first { it is CircuitStatus.Connected } }
            ?.let { TransportConnectionState.Connected(routingId) }
            ?: TransportConnectionState.Failed("No circuit came up")

    private suspend fun onConnected(
        endpoint: NodeEndpoint,
        routingId: String
    ) {
        failedNodeTracker.recordSuccess(endpoint.nodeId)
        connectedNodeId = endpoint.nodeId
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
                if (error is CancellationException) throw error
                if (error.isRecoverableConnectivityFailure() || error.isUnavailableNodeDirectory()) {
                    logger.debug { "Control-plane discovery unavailable: ${error.message}" }
                } else {
                    logger.error(error) { "Control-plane discovery failed" }
                }
            }
    }

    private fun onConnectedNodeEnded(
        endpoint: NodeEndpoint,
        state: TransportConnectionState
    ) {
        connectedNodeId = null
        mutableConnectionState.value =
            if (state is TransportConnectionState.Failed) {
                state
            } else {
                TransportConnectionState.Disconnected
            }

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
        // The WebSocket client already reports unexpected causes at their source.
        // Connection refusal/timeout is represented by the existing offline hint;
        // do not report this derived retry state as a new application error.
        logger.debug { "Transport connection unavailable; retrying: $message" }
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
                                returnToPreferredNode(connected = endpoint, endpoints = endpoints)
                            },
                            onFailure = { error ->
                                if (error is CancellationException) throw error
                                if (error.isRecoverableConnectivityFailure() || error.isUnavailableNodeDirectory()) {
                                    logger.debug { "Signed node directory refresh unavailable: ${error.message}" }
                                } else {
                                    // Discovery is optional while the existing connection is active.
                                    // Keep the verified cached route; never publish a transient
                                    // directory refresh failure as an app-wide transport error.
                                    logger.warn { "Signed node directory refresh rejected: ${error.message}" }
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
                                if (error is CancellationException) throw error
                                if (error.isRecoverableConnectivityFailure() || error.isUnavailableNodeDirectory()) {
                                    logger.debug { "Control-plane discovery refresh unavailable: ${error.message}" }
                                } else {
                                    logger.error(error) { "Control-plane discovery refresh failed" }
                                }
                            }
                    }
                }

            try {
                merge(
                    circuits.status
                        .filterIsInstance<CircuitStatus.Unavailable>()
                        .map { TransportConnectionState.Failed("The circuits to ${endpoint.nodeId} closed") },
                    reselect.receiveAsFlow().map { TransportConnectionState.Disconnected }
                ).first()
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
        const val ALIAS_CLAIM_INTERVAL_MILLISECONDS = 6L * 60L * 60L * 1_000L
        const val ALIAS_CLAIM_RETRY_MILLISECONDS = 60_000L
    }
}
