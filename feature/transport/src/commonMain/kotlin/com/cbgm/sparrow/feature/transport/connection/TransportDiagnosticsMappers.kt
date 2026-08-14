package com.cbgm.sparrow.feature.transport.connection

import com.cbgm.sparrow.core.transport.TransportDiagnosticConnectionState
import com.cbgm.sparrow.core.transport.TransportNodeDiagnosticState
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpoint

internal fun NodeEndpoint.diagnosticState(
    currentNodeId: String?,
    cooldownUntilEpochMillisecondsByNodeId: Map<String, Long>
): TransportNodeDiagnosticState =
    when {
        nodeId == currentNodeId -> TransportNodeDiagnosticState.CURRENT
        nodeId in cooldownUntilEpochMillisecondsByNodeId -> TransportNodeDiagnosticState.COOLDOWN
        else -> TransportNodeDiagnosticState.AVAILABLE
    }

internal fun TransportConnectionState.toDiagnosticState(): TransportDiagnosticConnectionState =
    when (this) {
        TransportConnectionState.Disconnected -> TransportDiagnosticConnectionState.DISCONNECTED
        TransportConnectionState.Connecting -> TransportDiagnosticConnectionState.CONNECTING
        is TransportConnectionState.Connected -> TransportDiagnosticConnectionState.CONNECTED
        is TransportConnectionState.Failed -> TransportDiagnosticConnectionState.FAILED
    }

internal fun TransportConnectionState.disconnectReason(): String =
    when (this) {
        is TransportConnectionState.Failed -> message
        else -> "Disconnected"
    }
