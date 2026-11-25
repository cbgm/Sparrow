package com.cbgm.sparrow.feature.transport.data.connection

import com.cbgm.sparrow.feature.transport.domain.model.NodeEndpoint
import com.cbgm.sparrow.feature.transport.domain.model.TransportConnectionState
import com.cbgm.sparrow.feature.transport.domain.model.TransportDiagnosticConnectionState
import com.cbgm.sparrow.feature.transport.domain.model.TransportNodeDiagnosticState

internal fun NodeEndpoint.diagnosticState(
    currentNodeId: String?,
    cooldownUntilEpochMillisecondsByNodeId: Map<String, Long>
): TransportNodeDiagnosticState =
    when {
        nodeId == currentNodeId -> TransportNodeDiagnosticState.CURRENT
        nodeId in cooldownUntilEpochMillisecondsByNodeId -> TransportNodeDiagnosticState.COOLDOWN
        else -> TransportNodeDiagnosticState.AVAILABLE
    }

internal fun TransportConnectionState.toTransportDiagnosticConnectionState(): TransportDiagnosticConnectionState =
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
