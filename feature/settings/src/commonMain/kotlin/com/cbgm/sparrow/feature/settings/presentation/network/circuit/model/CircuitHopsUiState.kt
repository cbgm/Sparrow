package com.cbgm.sparrow.feature.settings.presentation.network.circuit.model

import com.cbgm.sparrow.feature.transport.background.ConnectionMode

data class CircuitHopsUiState(
    val selectedHopCount: Int = 2,
    val minimumHopCount: Int = 1,
    val maximumHopCount: Int = 5,
    val status: CircuitUiStatus = CircuitUiStatus.Building,
    /** Round trip expected with [selectedHopCount], from the measured time per hop; null before a measurement. */
    val estimatedLatencyMilliseconds: Long? = null,
    /** Requests go out directly while there is no circuit (true) or wait for one. */
    val sendsDirectlyWithoutCircuit: Boolean = true,
    val selectedCircuitCount: Int = 2,
    val minimumCircuitCount: Int = 1,
    val maximumCircuitCount: Int = 3,
    /** How messages reach the device while Sparrow is closed. */
    val connectionMode: ConnectionMode = ConnectionMode.WHILE_OPEN,
    /** False on iOS: there only a note is shown. */
    val connectsWhileClosed: Boolean = true
)

sealed interface CircuitUiStatus {
    data object Building : CircuitUiStatus

    data object Unavailable : CircuitUiStatus

    data class Connected(
        val hopCount: Int,
        val requestedHopCount: Int,
        val latencyMilliseconds: Long,
        val circuitsUp: Int = 1,
        val requestedCircuitCount: Int = 1,
        /** At how many different operators the open circuits end. */
        val sessionOperators: Int = 1
    ) : CircuitUiStatus
}
