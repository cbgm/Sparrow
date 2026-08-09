package com.cbgm.securechat.feature.transport.connection

import com.cbgm.securechat.core.transport.TransportDiagnosticConnectionState
import com.cbgm.securechat.core.transport.TransportDiagnostics
import com.cbgm.securechat.core.transport.TransportNodeDiagnostic
import com.cbgm.securechat.feature.transport.discovery.NodeEndpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class RelayTransportDiagnosticsState(
    registryUrl: String?
) {
    private val _diagnostics =
        MutableStateFlow(
            TransportDiagnostics(
                registryUrl = registryUrl
            )
        )

    private var resolvedEndpoints: List<NodeEndpoint> = emptyList()
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
        registryAuthorityVerified: Boolean
    ) {
        resolvedEndpoints = endpoints
        updateNodes(
            currentNodeId = _diagnostics.value.currentNodeId,
            cooldownUntilEpochMillisecondsByNodeId = cooldownUntilEpochMillisecondsByNodeId
        )
        _diagnostics.value =
            _diagnostics.value.copy(
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

    private fun updateNodes(
        currentNodeId: String?,
        cooldownUntilEpochMillisecondsByNodeId: Map<String, Long>
    ) {
        _diagnostics.value =
            _diagnostics.value.copy(
                availableNodes =
                    resolvedEndpoints.map { endpoint ->
                        val cooldownUntilEpochMilliseconds =
                            cooldownUntilEpochMillisecondsByNodeId[endpoint.nodeId]

                        TransportNodeDiagnostic(
                            nodeId = endpoint.nodeId,
                            websocketUrl = endpoint.websocketUrl,
                            state =
                                endpoint.diagnosticState(
                                    currentNodeId = currentNodeId,
                                    cooldownUntilEpochMillisecondsByNodeId =
                                    cooldownUntilEpochMillisecondsByNodeId
                                ),
                            activeConnections = endpoint.activeConnections,
                            cooldownUntilEpochMilliseconds = cooldownUntilEpochMilliseconds
                        )
                    }
            )
    }
}
