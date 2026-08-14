package com.cbgm.sparrow.feature.settings.data.datasource

import android.content.Context
import androidx.core.content.edit

class AndroidSettingsStorage(
    context: Context
) : SettingsStorage {
    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    override suspend fun getLanguageTag(): String? = preferences.getString(KEY_LANGUAGE_TAG, null)

    override suspend fun setLanguageTag(
        languageTag: String
    ) {
        preferences
            .edit {
                putString(KEY_LANGUAGE_TAG, languageTag)
            }
    }

    override suspend fun getDeveloperModeEnabled(): Boolean = preferences.getBoolean(KEY_DEVELOPER_MODE_ENABLED, false)

    override suspend fun setDeveloperModeEnabled(
        enabled: Boolean
    ) {
        preferences
            .edit {
                putBoolean(KEY_DEVELOPER_MODE_ENABLED, enabled)
            }
    }

    override suspend fun getDirectIdentitySetupMode(): String? = preferences.getString(KEY_DIRECT_IDENTITY_SETUP_MODE, null)

    override suspend fun setDirectIdentitySetupMode(
        mode: String
    ) {
        preferences
            .edit {
                putString(KEY_DIRECT_IDENTITY_SETUP_MODE, mode)
            }
    }

    override suspend fun getBlockUnknownContactInvites(): Boolean = preferences.getBoolean(KEY_BLOCK_UNKNOWN_CONTACT_INVITES, false)

    override suspend fun setBlockUnknownContactInvites(
        enabled: Boolean
    ) {
        preferences
            .edit {
                putBoolean(KEY_BLOCK_UNKNOWN_CONTACT_INVITES, enabled)
            }
    }

    override suspend fun getBlockedContactIds(): String? = preferences.getString(KEY_BLOCKED_CONTACT_IDS, null)

    override suspend fun setBlockedContactIds(
        contactIds: String
    ) {
        preferences
            .edit {
                putString(KEY_BLOCKED_CONTACT_IDS, contactIds)
            }
    }

    override suspend fun clear() {
        preferences.edit { clear() }
    }

    private companion object {
        const val PREFERENCES_NAME = "sparrow_settings"
        const val KEY_LANGUAGE_TAG = "language_tag"
        const val KEY_DEVELOPER_MODE_ENABLED = "developer_mode_enabled"
        const val KEY_DIRECT_IDENTITY_SETUP_MODE = "direct_identity_setup_mode"
        const val KEY_BLOCK_UNKNOWN_CONTACT_INVITES = "block_unknown_contact_invites"
        const val KEY_BLOCKED_CONTACT_IDS = "blocked_contact_ids"
    }
}
