package com.cbgm.sparrow.feature.transport

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
) {
    /** HTTP base URL of the connected node: its WebSocket URL without the gateway path. */
    val currentNodeBaseUrl: String?
        get() {
            val websocketUrl = currentWebSocketUrl ?: return null
            val httpUrl =
                when {
                    websocketUrl.startsWith("wss://") -> "https://" + websocketUrl.removePrefix("wss://")
                    websocketUrl.startsWith("ws://") -> "http://" + websocketUrl.removePrefix("ws://")
                    else -> return null
                }
            return httpUrl.substringBefore("/v1/gateway").trimEnd('/')
        }
}

interface TransportDiagnosticsProvider {
    val diagnostics: StateFlow<TransportDiagnostics>
    val refreshDiagnostics: suspend () -> Unit
}
