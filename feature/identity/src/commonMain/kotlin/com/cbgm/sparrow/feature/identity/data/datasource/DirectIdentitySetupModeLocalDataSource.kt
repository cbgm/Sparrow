package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode

/** How a new direct contact gets this device's identity, kept on the device. */
internal class DirectIdentitySetupModeLocalDataSource(
    private val dataStore: SparrowDataStore
) {
    /** Nothing stored or an unknown value means the default, automatic invitation. */
    suspend fun get(): DirectIdentitySetupMode {
        val stored = dataStore.getString(KEY_DIRECT_IDENTITY_SETUP_MODE)
        return DirectIdentitySetupMode.entries.firstOrNull { candidate -> candidate.name == stored }
            ?: DirectIdentitySetupMode.AUTOMATIC_INVITATION
    }

    suspend fun set(mode: DirectIdentitySetupMode) {
        dataStore.edit { putString(KEY_DIRECT_IDENTITY_SETUP_MODE, mode.name) }
    }

    suspend fun clear() {
        dataStore.edit { removeString(KEY_DIRECT_IDENTITY_SETUP_MODE) }
    }

    private companion object {
        // The key Settings used when it stored the mode, so a saved choice stays.
        const val KEY_DIRECT_IDENTITY_SETUP_MODE = "settings.direct_identity_setup_mode"
    }
}
