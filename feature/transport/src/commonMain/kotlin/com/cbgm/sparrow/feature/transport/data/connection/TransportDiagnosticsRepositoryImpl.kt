package com.cbgm.sparrow.feature.transport.data.connection

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.transport.data.TransportConfig
import com.cbgm.sparrow.feature.transport.data.controlplane.ControlPlaneLocalDataSource
import com.cbgm.sparrow.feature.transport.data.directory.FailedNodeTracker
import com.cbgm.sparrow.feature.transport.data.directory.NodeEndpointRemoteDataSource
import com.cbgm.sparrow.feature.transport.data.routing.RoutingIdLocalDataSource
import com.cbgm.sparrow.feature.transport.domain.model.NodeEndpoint
import com.cbgm.sparrow.feature.transport.domain.model.TransportConnectionState
import com.cbgm.sparrow.feature.transport.domain.model.TransportDiagnostics
import com.cbgm.sparrow.feature.transport.domain.repository.TransportDiagnosticsRepository
import com.cbgm.sparrow.feature.transport.domain.rule.isRecoverableConnectivityFailure
import com.cbgm.sparrow.feature.transport.domain.rule.isUnavailableNodeDirectory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow

/**
 * What the home node connection knows about the gateway nodes: which of them failed lately (and cool down for a
 * while), and the diagnostics the developer menu shows. Every change of the connection is reported here.
 */
internal class TransportDiagnosticsRepositoryImpl(
    private val routingIdDataSource: RoutingIdLocalDataSource,
    private val nodeEndpointDataSource: NodeEndpointRemoteDataSource,
    private val controlPlaneConfiguration: ControlPlaneLocalDataSource,
    transportConfig: TransportConfig
) : TransportDiagnosticsRepository {
    private val logger = SparrowLog.withTag("TransportDiagnosticsRepositoryImpl")
    private var resolvedEndpoints: List<NodeEndpoint> = emptyList()

    private val failedNodeTracker =
        FailedNodeTracker(
            cooldownMilliseconds = transportConfig.failedNodeCooldownMilliseconds,
            now = SystemClock::nowEpochMilliseconds
        )

    private val state =
        TransportDiagnosticsState(
            registryUrl = controlPlaneConfiguration.activeEndpoint.value?.baseUrl,
            missingNodeCooldownMilliseconds = transportConfig.failedNodeCooldownMilliseconds,
            now = SystemClock::nowEpochMilliseconds
        )

    override val diagnostics: StateFlow<TransportDiagnostics> = state.diagnostics

    override suspend fun refreshDiagnostics() {
        routingIdDataSource.getLocalRoutingId()
            .onFailure { failure -> logger.error(failure) { "Could not obtain local routing ID for diagnostics" } }
            .getOrNull()?.let { routingId ->
                nodeEndpointDataSource
                    .resolve(
                        localRoutingId = routingId,
                        forceRefresh = true
                    ).fold(
                        onSuccess = ::resolved,
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

    /** The nodes of the signed directory that did not fail lately, in connection order. */
    fun available(endpoints: List<NodeEndpoint>): List<NodeEndpoint> = failedNodeTracker.available(endpoints)

    /** The node to try anyway while every node cools down. */
    fun probeCandidate(endpoints: List<NodeEndpoint>): NodeEndpoint? = failedNodeTracker.probeCandidate(endpoints)

    fun coolingDownNodeIds(endpoints: List<NodeEndpoint>): Set<String> = failedNodeTracker.unavailableNodeIds(endpoints)

    fun resolved(endpoints: List<NodeEndpoint>) {
        resolvedEndpoints = endpoints
        state.resolved(
            endpoints = endpoints,
            cooldownUntilEpochMillisecondsByNodeId = cooldowns(endpoints),
            registryAuthorityVerified = true,
            registryUrl = controlPlaneConfiguration.activeEndpoint.value?.baseUrl
        )
    }

    fun connecting() = state.connecting()

    fun selected(endpoint: NodeEndpoint) = state.selected(endpoint = endpoint, cooldownUntilEpochMillisecondsByNodeId = cooldowns(resolvedEndpoints))

    fun connected(endpoint: NodeEndpoint) {
        failedNodeTracker.recordSuccess(endpoint.nodeId)
        state.connected(endpoint = endpoint, cooldownUntilEpochMillisecondsByNodeId = cooldowns(resolvedEndpoints))
    }

    /** An attempt through [endpoint] failed (null: before a node was chosen); the node cools down for a while. */
    fun failed(
        endpoint: NodeEndpoint?,
        message: String
    ) {
        endpoint?.let { failedNodeTracker.recordFailure(it.nodeId) }
        state.failed(endpoint = endpoint, message = message, cooldownUntilEpochMillisecondsByNodeId = cooldowns(resolvedEndpoints))
    }

    /** The time at [endpoint] ended; only a failure lets the node cool down. */
    fun ended(
        endpoint: NodeEndpoint,
        endState: TransportConnectionState
    ) {
        if (endState is TransportConnectionState.Failed) failedNodeTracker.recordFailure(endpoint.nodeId)
        state.connectedNodeEnded(endpoint = endpoint, state = endState, cooldownUntilEpochMillisecondsByNodeId = cooldowns(resolvedEndpoints))
    }

    fun stopped() = state.stopped(cooldownUntilEpochMillisecondsByNodeId = cooldowns(resolvedEndpoints))

    private fun cooldowns(endpoints: List<NodeEndpoint>) = failedNodeTracker.cooldownUntilEpochMillisecondsByNodeId(endpoints)
}
