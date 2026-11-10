package com.cbgm.sparrow.feature.settings.presentation.network.circuit.model

import com.cbgm.sparrow.feature.transport.background.ConnectionMode

sealed interface CircuitHopsUiEvent {
    data object BackClicked : CircuitHopsUiEvent

    /** The slider moves; nothing is saved until [HopCountChosen]. */
    data class HopCountChanged(
        val hopCount: Int
    ) : CircuitHopsUiEvent

    data object HopCountChosen : CircuitHopsUiEvent

    data class CircuitCountChanged(
        val circuitCount: Int
    ) : CircuitHopsUiEvent

    data class ConnectionModeChanged(
        val mode: ConnectionMode
    ) : CircuitHopsUiEvent

    data class SendDirectlyChanged(
        val sendsDirectly: Boolean
    ) : CircuitHopsUiEvent
}
