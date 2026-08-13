package com.cbgm.securechat.feature.settings.domain.usecase

import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.security.DirectIdentitySetupModeRepository

class SetDirectIdentitySetupModeUseCase(
    private val repository: DirectIdentitySetupModeRepository
) {
    suspend operator fun invoke(mode: DirectIdentitySetupMode) {
        repository.setMode(mode)
    }
}
