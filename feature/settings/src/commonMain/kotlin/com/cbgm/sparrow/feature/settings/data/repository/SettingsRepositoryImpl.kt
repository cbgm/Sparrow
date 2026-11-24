package com.cbgm.sparrow.feature.settings.data.repository

import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.core.ui.theme.AppThemeMode
import com.cbgm.sparrow.core.util.result.safeCall
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.settings.data.datasource.SettingsStorage
import com.cbgm.sparrow.feature.settings.device.BuildInfoSource
import com.cbgm.sparrow.feature.settings.device.SystemLanguageSource
import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo
import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

class SettingsRepositoryImpl(
    private val buildInfoSource: BuildInfoSource,
    private val settingsStorage: SettingsStorage,
    private val systemLanguageSource: SystemLanguageSource
) : SettingsRepository {
    override suspend fun getLanguage(): Result<AppLanguage> =
        safeSuspendCall {
            val savedLanguageTag = settingsStorage.getLanguageTag()
            if (savedLanguageTag != null) {
                return@safeSuspendCall AppLanguage.fromLanguageTag(savedLanguageTag)
            }

            val systemLanguageTag = systemLanguageSource.getLanguageTag()
            val initialLanguage =
                AppLanguage.fromLanguageTag(
                    systemLanguageTag.substringBefore('-').substringBefore('_').lowercase()
                )
            settingsStorage.setLanguageTag(initialLanguage.languageTag)
            initialLanguage
        }

    override suspend fun setLanguage(
        language: AppLanguage
    ): Result<Unit> =
        safeSuspendCall {
            settingsStorage.setLanguageTag(
                languageTag = language.languageTag
            )
        }

    override suspend fun getThemeMode(): Result<AppThemeMode> =
        safeSuspendCall {
            AppThemeMode.entries.firstOrNull { mode -> mode.name == settingsStorage.getThemeMode() } ?: AppThemeMode.SYSTEM
        }

    override suspend fun setThemeMode(mode: AppThemeMode): Result<Unit> =
        safeSuspendCall { settingsStorage.setThemeMode(mode.name) }

    override suspend fun isDeveloperModeEnabled(): Result<Boolean> =
        safeSuspendCall { settingsStorage.getDeveloperModeEnabled() }

    override suspend fun setDeveloperModeEnabled(
        enabled: Boolean
    ): Result<Unit> =
        safeSuspendCall {
            settingsStorage.setDeveloperModeEnabled(
                enabled = enabled
            )
        }

    override suspend fun clearLocalData(): Result<Unit> =
        safeSuspendCall {
            settingsStorage.clear()
        }

    override fun getBuildInfo(): Result<BuildInfo> =
        safeCall { buildInfoSource.build }
}
