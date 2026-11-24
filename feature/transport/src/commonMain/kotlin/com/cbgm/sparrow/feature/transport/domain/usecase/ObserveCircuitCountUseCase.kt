package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.repository.TransportPreferencesRepository
import kotlinx.coroutines.flow.Flow

/** How many circuits the user chose to keep open at once. */
class ObserveCircuitCountUseCase(
    private val preferences: TransportPreferencesRepository
) {
    operator fun invoke(): Flow<Int> = preferences.observeCircuitCount()
}
