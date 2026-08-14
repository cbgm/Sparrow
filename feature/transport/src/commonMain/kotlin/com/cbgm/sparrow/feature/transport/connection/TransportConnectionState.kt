package com.cbgm.sparrow.feature.transport.connection

sealed interface TransportConnectionState {
    data object Disconnected : TransportConnectionState

    data object Connecting : TransportConnectionState

    data class Connected(
        val routingId: String
    ) : TransportConnectionState

    data class Failed(
        val message: String
    ) : TransportConnectionState
}
