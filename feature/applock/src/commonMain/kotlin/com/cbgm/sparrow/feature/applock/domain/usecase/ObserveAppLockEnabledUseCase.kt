package com.cbgm.sparrow.feature.applock.domain.usecase

import com.cbgm.sparrow.feature.applock.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow

class ObserveAppLockEnabledUseCase(
    private val repository: AppLockRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.observeEnabled()
}
