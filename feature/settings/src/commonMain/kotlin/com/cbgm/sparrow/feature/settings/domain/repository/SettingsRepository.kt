package com.cbgm.sparrow.feature.settings.domain.repository

import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo

interface SettingsRepository {
    suspend fun getLanguage(): AppLanguage

    suspend fun setLanguage(
        language: AppLanguage
    )

    suspend fun isDeveloperModeEnabled(): Boolean

    suspend fun setDeveloperModeEnabled(enabled: Boolean)

    suspend fun clearLocalData()

    fun getBuildInfo(): BuildInfo
}
