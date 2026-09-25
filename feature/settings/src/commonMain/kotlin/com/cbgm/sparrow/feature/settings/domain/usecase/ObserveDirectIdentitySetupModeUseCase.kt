package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentitySetupModeRepository
import kotlinx.coroutines.flow.Flow

class ObserveDirectIdentitySetupModeUseCase(
    private val repository: DirectIdentitySetupModeRepository
) {
    operator fun invoke(): Flow<DirectIdentitySetupMode> = repository.observeMode()
}
