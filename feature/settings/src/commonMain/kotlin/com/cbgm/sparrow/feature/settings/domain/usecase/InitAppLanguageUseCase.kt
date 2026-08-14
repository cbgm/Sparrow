package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.core.ui.locale.setAppLanguage
import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

class InitAppLanguageUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke() {
        val language = settingsRepository.getLanguage()

        setAppLanguage(
            language = language
        )
    }
}
