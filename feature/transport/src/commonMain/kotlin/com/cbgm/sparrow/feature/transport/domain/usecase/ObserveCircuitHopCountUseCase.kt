package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.repository.TransportPreferencesRepository
import kotlinx.coroutines.flow.Flow

/** How many hops the user chose for their circuits. */
class ObserveCircuitHopCountUseCase(
    private val preferences: TransportPreferencesRepository
) {
    operator fun invoke(): Flow<Int> = preferences.observeHopCount()
}
