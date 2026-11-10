package com.cbgm.sparrow.feature.settings.presentation.network.circuit.model

data class CircuitHopsUiState(
    val selectedHopCount: Int = 2,
    val minimumHopCount: Int = 1,
    val maximumHopCount: Int = 5,
    val status: CircuitUiStatus = CircuitUiStatus.Building,
    /** Round trip expected with [selectedHopCount], from the measured time per hop; null before a measurement. */
    val estimatedLatencyMilliseconds: Long? = null,
    /** Requests go out directly while there is no circuit (true) or wait for one. */
    val sendsDirectlyWithoutCircuit: Boolean = true,
    val selectedCircuitCount: Int = 1,
    val minimumCircuitCount: Int = 1,
    val maximumCircuitCount: Int = 3
)

sealed interface CircuitUiStatus {
    data object Building : CircuitUiStatus

    data object Unavailable : CircuitUiStatus

    data class Connected(
        val hopCount: Int,
        val requestedHopCount: Int,
        val latencyMilliseconds: Long,
        val circuitsUp: Int = 1,
        val requestedCircuitCount: Int = 1
    ) : CircuitUiStatus
}
