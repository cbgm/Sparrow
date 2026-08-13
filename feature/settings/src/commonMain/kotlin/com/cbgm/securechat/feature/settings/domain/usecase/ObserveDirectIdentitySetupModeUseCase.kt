package com.cbgm.securechat.feature.settings.domain.usecase

import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.security.DirectIdentitySetupModeRepository
import kotlinx.coroutines.flow.Flow

class ObserveDirectIdentitySetupModeUseCase(
    private val repository: DirectIdentitySetupModeRepository
) {
    operator fun invoke(): Flow<DirectIdentitySetupMode> = repository.observeMode()
}
