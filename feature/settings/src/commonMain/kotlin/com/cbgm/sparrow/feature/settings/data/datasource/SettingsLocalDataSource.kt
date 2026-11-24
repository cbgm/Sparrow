package com.cbgm.sparrow.feature.settings.data.datasource

import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.settings.data.mapper.toDirectIdentitySetupMode
import com.cbgm.sparrow.protocol.identity.DirectIdentitySetupModeSource

interface SettingsLocalDataSource : DirectIdentitySetupModeSource {
    suspend fun getLanguageTag(): String?

    suspend fun setLanguageTag(languageTag: String)

    suspend fun getThemeMode(): String?

    suspend fun setThemeMode(mode: String)

    suspend fun getDeveloperModeEnabled(): Boolean

    suspend fun setDeveloperModeEnabled(enabled: Boolean)

    suspend fun getDirectIdentitySetupMode(): String?

    suspend fun setDirectIdentitySetupMode(mode: String)

    override suspend fun sharesIdentityAutomatically(): Boolean =
        getDirectIdentitySetupMode().toDirectIdentitySetupMode() == DirectIdentitySetupMode.AUTOMATIC_INVITATION

    suspend fun getBlockUnknownContactInvites(): Boolean

    suspend fun setBlockUnknownContactInvites(enabled: Boolean)

    suspend fun getBlockedContactIds(): String?

    suspend fun setBlockedContactIds(contactIds: String)

    suspend fun clear()
}
