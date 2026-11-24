package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.Circuits

/** Builds the app's circuits and keeps them open while the caller runs. */
class RunCircuitsUseCase(
    private val circuits: Circuits
) {
    suspend operator fun invoke() = circuits.run()
}
