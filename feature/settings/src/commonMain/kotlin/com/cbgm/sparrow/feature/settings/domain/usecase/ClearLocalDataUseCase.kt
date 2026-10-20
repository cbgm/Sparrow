package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

class ClearLocalDataUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<Unit> = settingsRepository.clearLocalData()
}
