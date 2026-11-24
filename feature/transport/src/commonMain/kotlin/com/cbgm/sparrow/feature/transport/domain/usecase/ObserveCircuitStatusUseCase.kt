package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.Circuits
import com.cbgm.sparrow.feature.transport.domain.model.CircuitStatus
import kotlinx.coroutines.flow.StateFlow

/** Whether the circuit is up, through how many hops, and its last measured round trip. */
class ObserveCircuitStatusUseCase(
    private val circuitRunner: Circuits
) {
    operator fun invoke(): StateFlow<CircuitStatus> = circuitRunner.status
}
