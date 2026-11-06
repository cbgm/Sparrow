package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.circuit.CircuitStatus
import kotlinx.coroutines.flow.StateFlow

/** Whether the circuit is up, through how many hops, and its last measured round trip. */
class ObserveCircuitStatusUseCase(
    private val circuitRunner: CircuitRunner
) {
    operator fun invoke(): StateFlow<CircuitStatus> = circuitRunner.status
}
