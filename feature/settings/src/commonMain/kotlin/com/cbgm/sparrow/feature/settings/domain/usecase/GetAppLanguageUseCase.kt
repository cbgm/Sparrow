package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

class GetAppLanguageUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): AppLanguage = settingsRepository.getLanguage()
}
