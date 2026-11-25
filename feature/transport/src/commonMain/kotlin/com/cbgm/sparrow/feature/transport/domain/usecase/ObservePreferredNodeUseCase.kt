package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.repository.TransportPreferencesRepository
import kotlinx.coroutines.flow.Flow

/** The favourite gateway's node ID, or null for automatic selection. */
class ObservePreferredNodeUseCase(
    private val preferences: TransportPreferencesRepository
) {
    operator fun invoke(): Flow<String?> = preferences.observePreferredNode()
}
