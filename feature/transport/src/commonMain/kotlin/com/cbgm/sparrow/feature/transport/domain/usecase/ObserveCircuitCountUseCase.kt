package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.circuit.CircuitCountStore
import kotlinx.coroutines.flow.Flow

/** How many circuits the user chose to keep open at once. */
class ObserveCircuitCountUseCase(
    private val circuitCountStore: CircuitCountStore
) {
    operator fun invoke(): Flow<Int> = circuitCountStore.observe()
}
