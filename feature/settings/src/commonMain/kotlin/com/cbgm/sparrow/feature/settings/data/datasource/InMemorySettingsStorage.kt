package com.cbgm.sparrow.feature.settings.data.datasource

class InMemorySettingsStorage : SettingsStorage {
    private var languageTag: String? = null

    private var developerModeEnabled: Boolean = false

    private var directIdentitySetupMode: String? = null

    private var blockUnknownContactInvites: Boolean = false

    private var blockedContactIds: String? = null

    override suspend fun getLanguageTag(): String? = languageTag

    override suspend fun setLanguageTag(
        languageTag: String
    ) {
        this.languageTag = languageTag
    }

    override suspend fun getDeveloperModeEnabled(): Boolean = developerModeEnabled

    override suspend fun setDeveloperModeEnabled(
        enabled: Boolean
    ) {
        developerModeEnabled = enabled
    }

    override suspend fun getDirectIdentitySetupMode(): String? = directIdentitySetupMode

    override suspend fun setDirectIdentitySetupMode(
        mode: String
    ) {
        directIdentitySetupMode = mode
    }

    override suspend fun getBlockUnknownContactInvites(): Boolean = blockUnknownContactInvites

    override suspend fun setBlockUnknownContactInvites(
        enabled: Boolean
    ) {
        blockUnknownContactInvites = enabled
    }

    override suspend fun getBlockedContactIds(): String? = blockedContactIds

    override suspend fun setBlockedContactIds(
        contactIds: String
    ) {
        blockedContactIds = contactIds
    }

    override suspend fun clear() {
        languageTag = null
        developerModeEnabled = false
        directIdentitySetupMode = null
        blockUnknownContactInvites = false
        blockedContactIds = null
    }
}
