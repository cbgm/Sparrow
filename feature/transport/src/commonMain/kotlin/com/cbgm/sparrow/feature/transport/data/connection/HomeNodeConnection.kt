package com.cbgm.sparrow.feature.transport.data.connection

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.transport.data.route.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.domain.model.CircuitStatus
import com.cbgm.sparrow.feature.transport.domain.model.NodeEndpoint
import com.cbgm.sparrow.feature.transport.domain.model.TransportConnectionState
import com.cbgm.sparrow.feature.transport.domain.port.TransportConnectionPort
import com.cbgm.sparrow.feature.transport.domain.repository.NodeEndpointRepository
import com.cbgm.sparrow.feature.transport.domain.repository.RoutingIdRepository
import com.cbgm.sparrow.feature.transport.domain.repository.TransportPreferencesRepository
import com.cbgm.sparrow.feature.transport.domain.rule.PreferredNodeReturnPolicy
import com.cbgm.sparrow.feature.transport.domain.rule.isRecoverableConnectivityFailure
import com.cbgm.sparrow.feature.transport.domain.rule.isUnavailableNodeDirectory
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
 * previews and exchange rates, and counts as connected while a circuit is up. Around it [directoryRefresh] keeps the
 * node directory and the Control Plane discovery fresh and [aliasClaims] claims the phone alias. Messages go through
 * mailboxes; nothing arrives here.
 */
internal class HomeNodeConnection(
    private val circuits: CircuitRunner,
    private val aliasClaims: AliasClaimSchedule,
    private val routingIdRepository: RoutingIdRepository,
    private val nodeEndpointResolver: NodeEndpointRepository,
    private val directoryRefresh: NodeDirectoryRefresh,
    private val diagnostics: TransportDiagnosticsRepositoryImpl,
    private val preferences: TransportPreferencesRepository? = null
) : TransportConnectionPort {
    private val logger = SparrowLog.withTag("HomeNodeConnection")

    // IO, not Default: the many CPU-heavy startup tasks share Default and would delay connecting.
    private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connectionLoopJob: Job? = null
    private var aliasClaimJob: Job? = null

    /** Ends the time at the current home node, so the loop picks the node again (the preferred one first). */
    private val reselect = Channel<Unit>(Channel.CONFLATED)

    @Volatile
    override var connectedNodeId: String? = null
        private set

    private val mutableConnectionState =
        MutableStateFlow<TransportConnectionState>(TransportConnectionState.Disconnected)

    override val connectionState: StateFlow<TransportConnectionState> =
        mutableConnectionState.asStateFlow()

    override fun start() {
        if (connectionLoopJob?.isActive == true) {
            return
        }

        connectionLoopJob = connectionScope.launch { runConnectionLoop() }
        aliasClaimJob = connectionScope.launch { aliasClaims.run() }
    }

    override suspend fun stop() {
        val activeJob = connectionLoopJob

        connectionLoopJob = null
        directoryRefresh.cancel()
        aliasClaimJob?.cancelAndJoin()
        activeJob?.cancelAndJoin()
        connectedNodeId = null
        mutableConnectionState.value = TransportConnectionState.Disconnected
        diagnostics.stopped()
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
        val preferredNodeId = preferences?.observePreferredNode()?.first()
        val shouldReturn =
            PreferredNodeReturnPolicy.shouldReturn(
                preferredNodeId = preferredNodeId,
                connectedNodeId = connected.nodeId,
                endpoints = endpoints,
                coolingDownNodeIds = diagnostics.coolingDownNodeIds(endpoints)
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
            diagnostics.connecting()

            val routingId = routingIdRepository.getLocalRoutingId().getOrThrow()
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
            diagnostics.failed(endpoint = selectedEndpoint, message = error.message ?: "Transport connection error")
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
                directoryRefresh.refreshInBackground(connectionScope, routingId)
                cachedEndpoints
            } else {
                // resolveCached already rejected the cache, so skip checking it again and fetch.
                nodeEndpointResolver.resolve(routingId, forceRefresh = useCachedDirectory).getOrThrow()
            }
        val availableEndpoints = diagnostics.available(endpoints)

        diagnostics.resolved(endpoints)

        availableEndpoints.firstOrNull()?.let { return it }

        return checkNotNull(diagnostics.probeCandidate(endpoints)) {
            "Node directory does not contain a gateway node to probe"
        }.also { endpoint ->
            logger.warn {
                "Every discovered gateway node is cooling down; probing ${endpoint.nodeId}"
            }
        }
    }

    /** The circuits start at the home node from now on ([connectedNodeId] is their first hop). */
    private fun select(
        endpoint: NodeEndpoint,
        routingId: String
    ) {
        connectedNodeId = endpoint.nodeId
        diagnostics.selected(endpoint)
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
        connectedNodeId = endpoint.nodeId
        diagnostics.connected(endpoint)
        logger.info {
            "Transport connected through node ${endpoint.nodeId} as $routingId"
        }
        directoryRefresh
            .discoverControlPlanes(endpoint.websocketUrl)
            .onSuccess { count ->
                logger.info { "Control-plane discovery synchronized $count trusted addresses" }
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

        if (state !is TransportConnectionState.Failed) {
            logger.info {
                "Transport session on ${endpoint.nodeId} ended without a transport failure; " +
                    "retrying without node cooldown"
            }
        }
        diagnostics.ended(endpoint = endpoint, endState = state)
    }

    private fun onConnectionFailure(
        endpoint: NodeEndpoint,
        message: String
    ) {
        // The WebSocket client already reports unexpected causes at their source.
        // Connection refusal/timeout is represented by the existing offline hint;
        // do not report this derived retry state as a new application error.
        logger.debug { "Transport connection unavailable; retrying: $message" }
        diagnostics.failed(endpoint = endpoint, message = message)
    }

    /** The time at the home node ends when its circuits closed or the node is chosen again. */
    private suspend fun waitForConnectionEnd(
        routingId: String,
        endpoint: NodeEndpoint
    ): TransportConnectionState =
        coroutineScope {
            val refreshJob =
                launch {
                    directoryRefresh.whileConnected(routingId, endpoint) { endpoints ->
                        returnToPreferredNode(connected = endpoint, endpoints = endpoints)
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
            }
        }

    private companion object {
        const val CONNECTION_TIMEOUT_MILLISECONDS = 15_000L
        const val INITIAL_RECONNECT_DELAY_MILLISECONDS = 1_000L
        const val MAX_RECONNECT_DELAY_MILLISECONDS = 30_000L
    }
}
