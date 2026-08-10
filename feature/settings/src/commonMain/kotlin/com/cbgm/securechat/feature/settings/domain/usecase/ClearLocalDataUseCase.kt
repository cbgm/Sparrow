package com.cbgm.securechat.feature.settings.domain.usecase

import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository

class ClearLocalDataUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke() = settingsRepository.clearLocalData()
}
