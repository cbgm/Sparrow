package com.cbgm.sparrow.feature.applock.data.datasource

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.coroutines.flow.Flow

class AppLockSettingsDataSource(
    private val dataStore: SparrowDataStore
) {
    fun observeEnabled(): Flow<Boolean> =
        dataStore.observeBoolean(
            key = KEY_APP_LOCK_ENABLED,
            defaultValue = false
        )

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit {
            putBoolean(KEY_APP_LOCK_ENABLED, enabled)
        }
    }

    private companion object {
        const val KEY_APP_LOCK_ENABLED = "security.app_lock_enabled"
    }
}
