package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.core.ui.theme.AppThemeMode
import com.cbgm.sparrow.core.ui.theme.setAppThemeMode
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

class SetAppThemeModeUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(mode: AppThemeMode): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            settingsRepository.setThemeMode(mode).getOrThrow()
            setAppThemeMode(mode)
        }
}
