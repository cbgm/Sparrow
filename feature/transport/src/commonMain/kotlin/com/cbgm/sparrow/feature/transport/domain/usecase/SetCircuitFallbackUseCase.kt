package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.domain.repository.TransportPreferencesRepository

/** Chooses whether requests go out directly while there is no circuit, or wait for one. */
class SetCircuitFallbackUseCase(
    private val preferences: TransportPreferencesRepository
) {
    suspend operator fun invoke(sendsDirectly: Boolean): Result<Unit> = safeSuspendCall { preferences.setSendsDirectly(sendsDirectly) }
}
