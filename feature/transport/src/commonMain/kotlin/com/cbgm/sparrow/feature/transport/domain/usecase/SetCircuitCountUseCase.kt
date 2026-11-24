package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.domain.gateway.TransportPreferences

/** Saves how many circuits to keep open; the runner opens or closes circuits to match. */
class SetCircuitCountUseCase(
    private val preferences: TransportPreferences
) {
    suspend operator fun invoke(circuitCount: Int): Result<Unit> = safeSuspendCall { preferences.setCircuitCount(circuitCount) }
}
