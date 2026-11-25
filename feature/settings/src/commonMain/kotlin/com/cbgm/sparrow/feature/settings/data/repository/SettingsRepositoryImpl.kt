package com.cbgm.sparrow.feature.settings.data.repository

import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.core.ui.theme.AppThemeMode
import com.cbgm.sparrow.core.util.result.safeCall
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.settings.data.datasource.SettingsLocalDataSource
import com.cbgm.sparrow.feature.settings.device.BuildInfoLocalDataSource
import com.cbgm.sparrow.feature.settings.device.SystemLanguageLocalDataSource
import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo
import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

class SettingsRepositoryImpl(
    private val buildInfoDataSource: BuildInfoLocalDataSource,
    private val settingsDataSource: SettingsLocalDataSource,
    private val systemLanguageDataSource: SystemLanguageLocalDataSource
) : SettingsRepository {
    override suspend fun getLanguage(): Result<AppLanguage> =
        safeSuspendCall {
            val savedLanguageTag = settingsDataSource.getLanguageTag()
            if (savedLanguageTag != null) {
                return@safeSuspendCall AppLanguage.fromLanguageTag(savedLanguageTag)
            }

            val systemLanguageTag = systemLanguageDataSource.getLanguageTag()
            val initialLanguage =
                AppLanguage.fromLanguageTag(
                    systemLanguageTag.substringBefore('-').substringBefore('_').lowercase()
                )
            settingsDataSource.setLanguageTag(initialLanguage.languageTag)
            initialLanguage
        }

    override suspend fun setLanguage(
        language: AppLanguage
    ): Result<Unit> =
        safeSuspendCall {
            settingsDataSource.setLanguageTag(
                languageTag = language.languageTag
            )
        }

    override suspend fun getThemeMode(): Result<AppThemeMode> =
        safeSuspendCall {
            AppThemeMode.entries.firstOrNull { mode -> mode.name == settingsDataSource.getThemeMode() } ?: AppThemeMode.SYSTEM
        }

    override suspend fun setThemeMode(mode: AppThemeMode): Result<Unit> =
        safeSuspendCall { settingsDataSource.setThemeMode(mode.name) }

    override suspend fun isDeveloperModeEnabled(): Result<Boolean> =
        safeSuspendCall { settingsDataSource.getDeveloperModeEnabled() }

    override suspend fun setDeveloperModeEnabled(
        enabled: Boolean
    ): Result<Unit> =
        safeSuspendCall {
            settingsDataSource.setDeveloperModeEnabled(
                enabled = enabled
            )
        }

    override suspend fun clearLocalData(): Result<Unit> =
        safeSuspendCall {
            settingsDataSource.clear()
        }

    override fun getBuildInfo(): Result<BuildInfo> =
        safeCall { buildInfoDataSource.build }
}
