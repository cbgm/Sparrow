package com.cbgm.sparrow.feature.applock.domain.usecase

import com.cbgm.sparrow.feature.applock.domain.repository.AppLockRepository

class SetAppLockEnabledUseCase(
    private val repository: AppLockRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = repository.setEnabled(enabled)
}
