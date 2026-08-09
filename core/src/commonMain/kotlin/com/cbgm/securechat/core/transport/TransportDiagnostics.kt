package com.cbgm.securechat.core.transport

import kotlinx.coroutines.flow.StateFlow

enum class TransportDiagnosticConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED
}

enum class TransportNodeDiagnosticState {
    CURRENT,
    AVAILABLE,
    COOLDOWN
}

data class TransportNodeDiagnostic(
    val nodeId: String,
    val websocketUrl: String,
    val state: TransportNodeDiagnosticState,
    val activeConnections: Int = 0,
    val cooldownUntilEpochMilliseconds: Long? = null
)

data class TransportDiagnostics(
    val connectionState: TransportDiagnosticConnectionState = TransportDiagnosticConnectionState.DISCONNECTED,
    val currentNodeId: String? = null,
    val currentWebSocketUrl: String? = null,
    val registryUrl: String? = null,
    val registryAuthorityVerified: Boolean? = null,
    val availableNodes: List<TransportNodeDiagnostic> = emptyList(),
    val lastDisconnectReason: String? = null,
    val lastFailedNodeId: String? = null,
    val failoverCount: Int = 0
)

interface TransportDiagnosticsProvider {
    val diagnostics: StateFlow<TransportDiagnostics>
}
