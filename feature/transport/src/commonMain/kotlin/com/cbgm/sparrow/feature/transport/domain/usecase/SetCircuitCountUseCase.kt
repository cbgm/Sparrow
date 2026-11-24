package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.domain.repository.TransportPreferencesRepository

/** Saves how many circuits to keep open; the runner opens or closes circuits to match. */
class SetCircuitCountUseCase(
    private val preferences: TransportPreferencesRepository
) {
    suspend operator fun invoke(circuitCount: Int): Result<Unit> = safeSuspendCall { preferences.setCircuitCount(circuitCount) }
}
