package com.cbgm.securechat.feature.settings.data.datasource

interface SettingsStorage {
    suspend fun getLanguageTag(): String?

    suspend fun setLanguageTag(languageTag: String)

    suspend fun getDeveloperModeEnabled(): Boolean

    suspend fun setDeveloperModeEnabled(enabled: Boolean)

    suspend fun getDirectIdentitySetupMode(): String?

    suspend fun setDirectIdentitySetupMode(mode: String)

    suspend fun getBlockUnknownContactInvites(): Boolean

    suspend fun setBlockUnknownContactInvites(enabled: Boolean)

    suspend fun getBlockedContactIds(): String?

    suspend fun setBlockedContactIds(contactIds: String)

    suspend fun clear()
}
