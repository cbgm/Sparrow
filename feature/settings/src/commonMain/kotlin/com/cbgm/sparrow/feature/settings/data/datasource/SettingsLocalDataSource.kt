package com.cbgm.sparrow.feature.settings.data.datasource

interface SettingsLocalDataSource {
    suspend fun getLanguageTag(): String?

    suspend fun setLanguageTag(languageTag: String)

    suspend fun getThemeMode(): String?

    suspend fun setThemeMode(mode: String)

    suspend fun getDeveloperModeEnabled(): Boolean

    suspend fun setDeveloperModeEnabled(enabled: Boolean)

    suspend fun getBlockUnknownContactInvites(): Boolean

    suspend fun setBlockUnknownContactInvites(enabled: Boolean)

    suspend fun getBlockedContactIds(): String?

    suspend fun setBlockedContactIds(contactIds: String)

    suspend fun clear()
}
