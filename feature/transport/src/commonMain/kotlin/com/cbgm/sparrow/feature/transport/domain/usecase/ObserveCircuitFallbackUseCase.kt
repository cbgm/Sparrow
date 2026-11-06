package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.circuit.CircuitFallbackStore
import kotlinx.coroutines.flow.Flow

/** Whether requests go out directly while there is no circuit (true) or wait for one. */
class ObserveCircuitFallbackUseCase(
    private val fallbackStore: CircuitFallbackStore
) {
    operator fun invoke(): Flow<Boolean> = fallbackStore.observeSendsDirectly()
}
