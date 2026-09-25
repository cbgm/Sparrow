package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentitySetupModeRepository

class SetDirectIdentitySetupModeUseCase(
    private val repository: DirectIdentitySetupModeRepository
) {
    suspend operator fun invoke(mode: DirectIdentitySetupMode) {
        repository.setMode(mode)
    }
}
