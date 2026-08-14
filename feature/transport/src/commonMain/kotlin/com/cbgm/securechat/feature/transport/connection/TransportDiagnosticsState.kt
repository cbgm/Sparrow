package com.cbgm.securechat.feature.transport.connection

import com.cbgm.securechat.core.transport.TransportDiagnosticConnectionState
import com.cbgm.securechat.core.transport.TransportDiagnostics
import com.cbgm.securechat.core.transport.TransportNodeDiagnostic
import com.cbgm.securechat.core.transport.TransportNodeDiagnosticState
import com.cbgm.securechat.feature.transport.discovery.NodeEndpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class TransportDiagnosticsState(
    registryUrl: String?,
    private val missingNodeCooldownMilliseconds: Long,
    private val now: () -> Long
) {
    private val _diagnostics =
        MutableStateFlow(
            TransportDiagnostics(
                registryUrl = registryUrl
            )
        )

    private var resolvedEndpoints: List<NodeEndpoint> = emptyList()
    private val missingEndpoints = mutableMapOf<String, MissingEndpoint>()
    private var failedConnectedNodeId: String? = null

    val diagnostics: StateFlow<TransportDiagnostics> = _diagnostics.asStateFlow()

    fun connecting() {
        _diagnostics.value =
            _diagnostics.value.copy(
                connectionState = TransportDiagnosticConnectionState.CONNECTING,
                currentNodeId = null,
                currentWebSocketUrl = null
            )
    }

    fun resolved(
        endpoints: List<NodeEndpoint>,
        cooldownUntilEpochMillisecondsByNodeId: Map<String, Long>,
        registryAuthorityVerified: Boolean,
        registryUrl: String?
    ) {
        updateResolvedEndpoints(endpoints)
        updateNodes(
            currentNodeId = _diagnostics.value.currentNodeId,
            cooldownUntilEpochMillisecondsByNodeId = cooldownUntilEpochMillisecondsByNodeId
        )
        _diagnostics.value =
            _diagnostics.value.copy(
                registryUrl = registryUrl,
                registryAuthorityVerified = registryAuthorityVerified
            )
    }

    fun selected(
        endpoint: NodeEndpoint,
        cooldownUntilEpochMillisecondsByNodeId: Map<String, Long>
    ) {
        _diagnostics.value =
            _diagnostics.value.copy(
                currentNodeId = endpoint.nodeId,
                currentWebSocketUrl = endpoint.websocketUrl
            )
        updateNodes(
            currentNodeId = endpoint.nodeId,
            cooldownUntilEpochMillisecondsByNodeId = cooldownUntilEpochMillisecondsByNodeId
        )
    }

    fun connected(
        endpoint: NodeEndpoint,
        cooldownUntilEpochMillisecondsByNodeId: Map<String, Long>
    ) {
        val failoverOccurred =
            failedConnectedNodeId != null && failedConnectedNodeId != endpoint.nodeId

        failedConnectedNodeId = null

        _diagnostics.value =
            _diagnostics.value.copy(
                connectionState = TransportDiagnosticConnectionState.CONNECTED,
                currentNodeId = endpoint.nodeId,
                currentWebSocketUrl = endpoint.websocketUrl,
                failoverCount = _diagnostics.value.failoverCount + if (failoverOccurred) 1 else 0
            )
        updateNodes(
            currentNodeId = endpoint.nodeId,
            cooldownUntilEpochMillisecondsByNodeId = cooldownUntilEpochMillisecondsByNodeId
        )
    }

    fun connectedNodeEnded(
        endpoint: NodeEndpoint,
        state: TransportConnectionState,
        cooldownUntilEpochMillisecondsByNodeId: Map<String, Long>
    ) {
        failedConnectedNodeId = endpoint.nodeId

        _diagnostics.value =
            _diagnostics.value.copy(
                connectionState = state.toDiagnosticState(),
                currentNodeId = null,
                currentWebSocketUrl = null,
                lastDisconnectReason = state.disconnectReason(),
                lastFailedNodeId = endpoint.nodeId
            )
        updateNodes(
            currentNodeId = null,
            cooldownUntilEpochMillisecondsByNodeId = cooldownUntilEpochMillisecondsByNodeId
        )
    }

    fun failed(
        endpoint: NodeEndpoint?,
        message: String,
        cooldownUntilEpochMillisecondsByNodeId: Map<String, Long>
    ) {
        _diagnostics.value =
            _diagnostics.value.copy(
                connectionState = TransportDiagnosticConnectionState.FAILED,
                currentNodeId = null,
                currentWebSocketUrl = null,
                lastDisconnectReason = message,
                lastFailedNodeId = endpoint?.nodeId ?: _diagnostics.value.lastFailedNodeId
            )
        updateNodes(
            currentNodeId = null,
            cooldownUntilEpochMillisecondsByNodeId = cooldownUntilEpochMillisecondsByNodeId
        )
    }

    fun stopped(cooldownUntilEpochMillisecondsByNodeId: Map<String, Long>) {
        _diagnostics.value =
            _diagnostics.value.copy(
                connectionState = TransportDiagnosticConnectionState.DISCONNECTED,
                currentNodeId = null,
                currentWebSocketUrl = null,
                lastDisconnectReason = "Stopped"
            )
        updateNodes(
            currentNodeId = null,
            cooldownUntilEpochMillisecondsByNodeId = cooldownUntilEpochMillisecondsByNodeId
        )
    }

    private fun updateResolvedEndpoints(endpoints: List<NodeEndpoint>) {
        val currentTime = now()
        val resolvedNodeIds = endpoints.mapTo(mutableSetOf(), NodeEndpoint::nodeId)

        missingEndpoints.entries.removeAll { (nodeId, missing) ->
            nodeId in resolvedNodeIds || missing.cooldownUntilEpochMilliseconds <= currentTime
        }
        resolvedEndpoints
            .filterNot { endpoint -> endpoint.nodeId in resolvedNodeIds }
            .forEach { endpoint ->
                missingEndpoints.putIfAbsent(
                    endpoint.nodeId,
                    MissingEndpoint(
                        endpoint = endpoint,
                        cooldownUntilEpochMilliseconds =
                            currentTime + missingNodeCooldownMilliseconds
                    )
                )
            }

        resolvedEndpoints = endpoints
    }

    private fun updateNodes(
        currentNodeId: String?,
        cooldownUntilEpochMillisecondsByNodeId: Map<String, Long>
    ) {
        val currentTime = now()
        missingEndpoints.entries.removeAll { (_, missing) ->
            missing.cooldownUntilEpochMilliseconds <= currentTime
        }

        val resolvedNodes =
            resolvedEndpoints.map { endpoint ->
                val cooldownUntilEpochMilliseconds =
                    cooldownUntilEpochMillisecondsByNodeId[endpoint.nodeId]

                val diagnosticState =
                    endpoint.diagnosticState(
                        currentNodeId = currentNodeId,
                        cooldownUntilEpochMillisecondsByNodeId =
                        cooldownUntilEpochMillisecondsByNodeId
                    )

                TransportNodeDiagnostic(
                    nodeId = endpoint.nodeId,
                    websocketUrl = endpoint.websocketUrl,
                    state = diagnosticState,
                    activeConnections =
                        if (diagnosticState == TransportNodeDiagnosticState.COOLDOWN) {
                            0
                        } else {
                            endpoint.activeConnections
                        },
                    cooldownUntilEpochMilliseconds = cooldownUntilEpochMilliseconds
                )
            }
        val missingNodes =
            missingEndpoints.values.map { missing ->
                TransportNodeDiagnostic(
                    nodeId = missing.endpoint.nodeId,
                    websocketUrl = missing.endpoint.websocketUrl,
                    state =
                        if (missing.endpoint.nodeId == currentNodeId) {
                            TransportNodeDiagnosticState.CURRENT
                        } else {
                            TransportNodeDiagnosticState.COOLDOWN
                        },
                    activeConnections = 0,
                    cooldownUntilEpochMilliseconds = missing.cooldownUntilEpochMilliseconds
                )
            }

        _diagnostics.value =
            _diagnostics.value.copy(
                availableNodes = resolvedNodes + missingNodes
            )
    }

    private data class MissingEndpoint(
        val endpoint: NodeEndpoint,
        val cooldownUntilEpochMilliseconds: Long
    )
}
