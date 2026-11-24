package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.core.ui.theme.AppThemeMode
import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

class GetAppThemeModeUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<AppThemeMode> = settingsRepository.getThemeMode()
}
