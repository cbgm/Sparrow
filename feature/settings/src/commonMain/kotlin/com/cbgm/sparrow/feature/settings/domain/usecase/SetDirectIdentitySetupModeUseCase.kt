package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository

class SetDirectIdentitySetupModeUseCase(
    private val repository: DirectIdentitySetupModeRepository
) {
    suspend operator fun invoke(mode: DirectIdentitySetupMode) {
        repository.setMode(mode)
    }
}
