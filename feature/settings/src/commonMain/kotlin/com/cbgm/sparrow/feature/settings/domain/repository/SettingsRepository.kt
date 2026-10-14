package com.cbgm.sparrow.feature.settings.domain.repository

import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo

interface SettingsRepository {
    suspend fun getLanguage(): Result<AppLanguage>

    suspend fun setLanguage(
        language: AppLanguage
    ): Result<Unit>

    suspend fun isDeveloperModeEnabled(): Result<Boolean>

    suspend fun setDeveloperModeEnabled(enabled: Boolean): Result<Unit>

    suspend fun clearLocalData(): Result<Unit>

    fun getBuildInfo(): Result<BuildInfo>
}
