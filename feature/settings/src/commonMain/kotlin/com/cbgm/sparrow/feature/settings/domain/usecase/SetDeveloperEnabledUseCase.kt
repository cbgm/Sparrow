package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

class SetDeveloperEnabledUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> =
        settingsRepository.setDeveloperModeEnabled(enabled)
}
