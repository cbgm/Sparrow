package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.model.CircuitStatus
import com.cbgm.sparrow.feature.transport.domain.port.CircuitPort
import kotlinx.coroutines.flow.StateFlow

/** Whether the circuit is up, through how many hops, and its last measured round trip. */
class ObserveCircuitStatusUseCase(
    private val circuitRunner: CircuitPort
) {
    operator fun invoke(): StateFlow<CircuitStatus> = circuitRunner.status
}
