package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.circuit.CircuitHopCountStore
import kotlinx.coroutines.flow.Flow

/** How many hops the user chose for their circuits. */
class ObserveCircuitHopCountUseCase(
    private val hopCountStore: CircuitHopCountStore
) {
    operator fun invoke(): Flow<Int> = hopCountStore.observe()
}
