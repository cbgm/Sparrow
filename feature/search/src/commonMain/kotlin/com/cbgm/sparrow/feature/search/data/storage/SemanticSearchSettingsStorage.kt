package com.cbgm.sparrow.feature.search.data.storage

import com.cbgm.sparrow.core.datastore.SparrowDataStore
import kotlinx.coroutines.flow.Flow

class SemanticSearchSettingsStorage(
    private val dataStore: SparrowDataStore
) {
    fun observeEnabled(): Flow<Boolean> = dataStore.observeBoolean(ENABLED_KEY)

    suspend fun isEnabled(): Boolean = dataStore.getBoolean(ENABLED_KEY)

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { putBoolean(ENABLED_KEY, enabled) }
    }

    private companion object {
        const val ENABLED_KEY = "search.semantic.enabled"
    }
}
