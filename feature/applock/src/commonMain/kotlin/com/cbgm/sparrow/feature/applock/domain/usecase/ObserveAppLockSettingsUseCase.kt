package com.cbgm.sparrow.feature.applock.domain.usecase

import com.cbgm.sparrow.feature.applock.domain.model.AppLockSettings
import com.cbgm.sparrow.feature.applock.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow

class ObserveAppLockSettingsUseCase(
    private val repository: AppLockRepository
) {
    operator fun invoke(): Flow<AppLockSettings> = repository.observeSettings()
}
