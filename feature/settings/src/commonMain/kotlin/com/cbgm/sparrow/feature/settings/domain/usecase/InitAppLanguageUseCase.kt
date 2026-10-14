package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.ui.locale.setAppLanguage
import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

class InitAppLanguageUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val language = settingsRepository.getLanguage().getOrThrow()

            setAppLanguage(
                language = language
            )
        }
}
