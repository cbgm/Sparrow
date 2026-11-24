package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.port.CircuitPort

/** Builds the app's circuits and keeps them open while the caller runs. */
class RunCircuitsUseCase(
    private val circuits: CircuitPort
) {
    suspend operator fun invoke() = circuits.run()
}
