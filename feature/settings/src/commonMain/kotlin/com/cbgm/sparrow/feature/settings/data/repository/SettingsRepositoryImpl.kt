package com.cbgm.sparrow.feature.settings.data.repository

import com.cbgm.sparrow.core.result.safeCall
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.feature.settings.data.datasource.SettingsStorage
import com.cbgm.sparrow.feature.settings.device.BuildInfoProvider
import com.cbgm.sparrow.feature.settings.device.SystemLanguageProvider
import com.cbgm.sparrow.feature.settings.domain.model.BuildInfo
import com.cbgm.sparrow.feature.settings.domain.repository.SettingsRepository

class SettingsRepositoryImpl(
    private val buildInfoProvider: BuildInfoProvider,
    private val settingsStorage: SettingsStorage,
    private val systemLanguageProvider: SystemLanguageProvider
) : SettingsRepository {
    override suspend fun getLanguage(): Result<AppLanguage> =
        safeSuspendCall {
            val savedLanguageTag = settingsStorage.getLanguageTag()
            if (savedLanguageTag != null) {
                return@safeSuspendCall AppLanguage.fromLanguageTag(savedLanguageTag)
            }

            val systemLanguageTag = systemLanguageProvider.getLanguageTag()
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
        safeCall { buildInfoProvider.build }
}
