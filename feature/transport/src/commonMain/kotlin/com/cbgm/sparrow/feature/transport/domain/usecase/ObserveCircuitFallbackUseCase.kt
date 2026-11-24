package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.TransportPreferences
import kotlinx.coroutines.flow.Flow

/** Whether requests go out directly while there is no circuit (true) or wait for one. */
class ObserveCircuitFallbackUseCase(
    private val preferences: TransportPreferences
) {
    operator fun invoke(): Flow<Boolean> = preferences.observeSendsDirectly()
}
