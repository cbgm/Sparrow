package com.cbgm.sparrow.feature.applock.domain.usecase

import com.cbgm.sparrow.feature.applock.domain.model.AppLockTimeout
import com.cbgm.sparrow.feature.applock.domain.repository.AppLockRepository

class SetAppLockTimeoutUseCase(
    private val repository: AppLockRepository
) {
    suspend operator fun invoke(timeout: AppLockTimeout): Result<Unit> = repository.setTimeout(timeout)
}
