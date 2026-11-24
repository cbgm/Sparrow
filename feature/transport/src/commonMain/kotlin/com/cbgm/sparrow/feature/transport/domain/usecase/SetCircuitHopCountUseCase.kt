package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.domain.gateway.TransportPreferences

/** Saves the chosen hop count; the running circuit is rebuilt with it. */
class SetCircuitHopCountUseCase(
    private val preferences: TransportPreferences
) {
    suspend operator fun invoke(hopCount: Int): Result<Unit> = safeSuspendCall { preferences.setHopCount(hopCount) }
}
