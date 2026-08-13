package com.cbgm.securechat.feature.settings.data.repository

import com.cbgm.securechat.core.ui.locale.AppLanguage
import com.cbgm.securechat.feature.settings.data.datasource.SettingsStorage
import com.cbgm.securechat.feature.settings.domain.model.BuildInfo
import com.cbgm.securechat.feature.settings.domain.repository.BuildInfoProviderRepository
import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository

class SettingsRepositoryImpl(
    private val buildInfoProvider: BuildInfoProviderRepository,
    private val settingsStorage: SettingsStorage
) : SettingsRepository {
    override suspend fun getLanguage(): AppLanguage =
        AppLanguage.fromLanguageTag(
            settingsStorage.getLanguageTag()
        )

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
