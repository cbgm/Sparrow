package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

/** Resets the app's settings, including how new contacts get the identity, to how they were after installing. */
class ClearLocalDataUseCase(
    private val settingsRepository: SettingsRepository,
    private val directIdentitySetupModeRepository: DirectIdentitySetupModeRepository
) {
    suspend operator fun invoke(): Result<Unit> =
        settingsRepository.clearLocalData().mapCatching { directIdentitySetupModeRepository.resetMode().getOrThrow() }
}
