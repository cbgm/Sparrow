package com.cbgm.sparrow.feature.settings.data.datasource

import com.cbgm.sparrow.core.datastore.SparrowDataStore

class SettingsStorageImpl(
    private val dataStore: SparrowDataStore
) : SettingsStorage {
    override suspend fun getLanguageTag(): String? = dataStore.getString(KEY_LANGUAGE_TAG)

    override suspend fun setLanguageTag(languageTag: String) {
        dataStore.edit { putString(KEY_LANGUAGE_TAG, languageTag) }
    }

    override suspend fun getDeveloperModeEnabled(): Boolean =
        dataStore.getBoolean(KEY_DEVELOPER_MODE_ENABLED)

    override suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        dataStore.edit { putBoolean(KEY_DEVELOPER_MODE_ENABLED, enabled) }
    }

    override suspend fun getDirectIdentitySetupMode(): String? =
        dataStore.getString(KEY_DIRECT_IDENTITY_SETUP_MODE)

    override suspend fun setDirectIdentitySetupMode(mode: String) {
        dataStore.edit { putString(KEY_DIRECT_IDENTITY_SETUP_MODE, mode) }
    }

    override suspend fun getBlockUnknownContactInvites(): Boolean =
        dataStore.getBoolean(KEY_BLOCK_UNKNOWN_CONTACT_INVITES)

    override suspend fun setBlockUnknownContactInvites(enabled: Boolean) {
        dataStore.edit { putBoolean(KEY_BLOCK_UNKNOWN_CONTACT_INVITES, enabled) }
    }

    override suspend fun getBlockedContactIds(): String? = dataStore.getString(KEY_BLOCKED_CONTACT_IDS)

    override suspend fun setBlockedContactIds(contactIds: String) {
        dataStore.edit { putString(KEY_BLOCKED_CONTACT_IDS, contactIds) }
    }

    override suspend fun clear() {
        dataStore.edit {
            removeString(KEY_LANGUAGE_TAG)
            removeBoolean(KEY_DEVELOPER_MODE_ENABLED)
            removeString(KEY_DIRECT_IDENTITY_SETUP_MODE)
            removeBoolean(KEY_BLOCK_UNKNOWN_CONTACT_INVITES)
            removeString(KEY_BLOCKED_CONTACT_IDS)
        }
    }

    private companion object {
        const val PREFIX = "settings."
        const val KEY_LANGUAGE_TAG = "${PREFIX}language_tag"
        const val KEY_DEVELOPER_MODE_ENABLED = "${PREFIX}developer_mode_enabled"
        const val KEY_DIRECT_IDENTITY_SETUP_MODE = "${PREFIX}direct_identity_setup_mode"
        const val KEY_BLOCK_UNKNOWN_CONTACT_INVITES = "${PREFIX}block_unknown_contact_invites"
        const val KEY_BLOCKED_CONTACT_IDS = "${PREFIX}blocked_contact_ids"
    }
}
