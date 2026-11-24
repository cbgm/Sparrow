package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.TransportPreferences
import kotlinx.coroutines.flow.Flow

/** The favourite gateway's node ID, or null for automatic selection. */
class ObservePreferredNodeUseCase(
    private val preferences: TransportPreferences
) {
    operator fun invoke(): Flow<String?> = preferences.observePreferredNode()
}
