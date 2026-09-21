package com.cbgm.sparrow.feature.transport.discovery

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.serialization.json.Json

class DataStoreNodeDirectoryCache(
    private val dataStore: SparrowDataStore,
    private val json: Json
) : NodeDirectoryCache {
    override suspend fun read(): CachedNodeDirectory? =
        dataStore.getString(DIRECTORY_KEY)?.let { encoded ->
            runCatching { json.decodeFromString<CachedNodeDirectory>(encoded) }
                .onFailure { error -> SparrowLog.error("DataStoreNodeDirectoryCache", "Saved node directory could not be decoded", error) }
                .getOrNull()
        }

    override suspend fun write(directory: CachedNodeDirectory) {
        dataStore.edit {
            putString(DIRECTORY_KEY, json.encodeToString(directory))
        }
    }

    private companion object {
        const val DIRECTORY_KEY = "transport.node_directory.signed_directory"
    }
}
