package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.domain.gateway.TransportPreferences

/** Saves how many decoy envelopes go out with each send; 0 switches them off. */
class SetDecoyCountUseCase(
    private val preferences: TransportPreferences
) {
    suspend operator fun invoke(decoyCount: Int): Result<Unit> = safeSuspendCall { preferences.setDecoyCount(decoyCount) }
}
