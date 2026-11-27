package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.core.ui.theme.setAppThemeMode
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

class InitAppThemeModeUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            setAppThemeMode(settingsRepository.getThemeMode().getOrThrow())
        }
}
