package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentitySetupModeRepository
import kotlinx.coroutines.flow.Flow

class ObserveIdentitySetupModeUseCase(
    private val repository: DirectIdentitySetupModeRepository
) {
    operator fun invoke(): Flow<DirectIdentitySetupMode> = repository.observeMode()
}
