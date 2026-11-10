package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.circuit.CircuitCountStore

/** Saves how many circuits to keep open; the runner opens or closes circuits to match. */
class SetCircuitCountUseCase(
    private val circuitCountStore: CircuitCountStore
) {
    suspend operator fun invoke(circuitCount: Int): Result<Unit> = safeSuspendCall { circuitCountStore.set(circuitCount) }
}
