package com.cbgm.sparrow.feature.transport.domain.model

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
