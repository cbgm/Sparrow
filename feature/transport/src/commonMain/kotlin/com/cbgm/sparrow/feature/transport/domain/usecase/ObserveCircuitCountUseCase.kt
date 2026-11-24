package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.TransportPreferences
import kotlinx.coroutines.flow.Flow

/** How many circuits the user chose to keep open at once. */
class ObserveCircuitCountUseCase(
    private val preferences: TransportPreferences
) {
    operator fun invoke(): Flow<Int> = preferences.observeCircuitCount()
}
