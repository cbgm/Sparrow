package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.core.ui.locale.setAppLanguage
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

class SetAppLanguageUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        language: AppLanguage
    ): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            settingsRepository.setLanguage(
                language
            ).getOrThrow()

            setAppLanguage(
                language
            )
        }
}
