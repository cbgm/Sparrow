package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import kotlinx.coroutines.flow.Flow

class ObserveIdentitySetupModeUseCase(
    private val repository: DirectIdentitySetupModeRepository
) {
    operator fun invoke(): Flow<DirectIdentitySetupMode> = repository.observeMode()
}
