package com.cbgm.sparrow.core.embedding.data.storage

import com.cbgm.sparrow.data.datastore.SparrowDataStore

class LocalEmbeddingSettingsStorage(
    private val dataStore: SparrowDataStore
) {
    suspend fun isSemanticSearchEnabled(): Boolean = dataStore.getBoolean(SEARCH_ENABLED_KEY)

    suspend fun isMessageSafetyEnabled(): Boolean = dataStore.getBoolean(SAFETY_ENABLED_KEY)

    suspend fun setSemanticSearchEnabled(enabled: Boolean) {
        dataStore.edit { putBoolean(SEARCH_ENABLED_KEY, enabled) }
    }

    suspend fun setMessageSafetyEnabled(enabled: Boolean) {
        dataStore.edit { putBoolean(SAFETY_ENABLED_KEY, enabled) }
    }

    private companion object {
        // Keep the existing key so users do not lose their semantic-search preference.
        const val SEARCH_ENABLED_KEY = "search.semantic.enabled"
        const val SAFETY_ENABLED_KEY = "safety.message.enabled"
    }
}
