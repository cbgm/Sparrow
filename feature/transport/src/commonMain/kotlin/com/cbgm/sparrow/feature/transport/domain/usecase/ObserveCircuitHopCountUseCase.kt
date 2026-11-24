package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.TransportPreferences
import kotlinx.coroutines.flow.Flow

/** How many hops the user chose for their circuits. */
class ObserveCircuitHopCountUseCase(
    private val preferences: TransportPreferences
) {
    operator fun invoke(): Flow<Int> = preferences.observeHopCount()
}
