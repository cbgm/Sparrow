package com.cbgm.sparrow.feature.transport.discovery

import com.cbgm.sparrow.core.datastore.SparrowDataStore
import kotlinx.serialization.json.Json

class DataStoreNodeDirectoryCache(
    private val dataStore: SparrowDataStore,
    private val json: Json
) : NodeDirectoryCache {
    override suspend fun read(): CachedNodeDirectory? =
        dataStore.getString(DIRECTORY_KEY)?.let { encoded ->
            runCatching { json.decodeFromString<CachedNodeDirectory>(encoded) }.getOrNull()
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
