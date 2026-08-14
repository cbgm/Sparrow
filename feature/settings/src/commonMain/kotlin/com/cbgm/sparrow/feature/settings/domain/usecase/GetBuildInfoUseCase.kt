package com.cbgm.sparrow.feature.settings.domain.usecase

import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo
import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

class GetBuildInfoUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): BuildInfo = settingsRepository.getBuildInfo()
}
