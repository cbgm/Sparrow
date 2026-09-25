package com.cbgm.sparrow.feature.settings.data.repository

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
    override suspend fun getLanguage(): AppLanguage {
        val savedLanguageTag = settingsStorage.getLanguageTag()
        if (savedLanguageTag != null) {
            return AppLanguage.fromLanguageTag(savedLanguageTag)
        }

        val systemLanguageTag = systemLanguageProvider.getLanguageTag()
        val initialLanguage =
            AppLanguage.fromLanguageTag(
                systemLanguageTag.substringBefore('-').substringBefore('_').lowercase()
            )
        settingsStorage.setLanguageTag(initialLanguage.languageTag)
        return initialLanguage
    }

    override suspend fun setLanguage(
        language: AppLanguage
    ) {
        settingsStorage.setLanguageTag(
            languageTag = language.languageTag
        )
    }

    override suspend fun isDeveloperModeEnabled(): Boolean = settingsStorage.getDeveloperModeEnabled()

    override suspend fun setDeveloperModeEnabled(
        enabled: Boolean
    ) {
        settingsStorage.setDeveloperModeEnabled(
            enabled = enabled
        )
    }

    override suspend fun clearLocalData() {
        settingsStorage.clear()
    }

    override fun getBuildInfo(): BuildInfo = buildInfoProvider.build
}
