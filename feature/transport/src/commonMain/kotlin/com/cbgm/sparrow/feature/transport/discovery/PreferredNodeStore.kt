package com.cbgm.sparrow.feature.transport.discovery

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.coroutines.flow.Flow

/** The one gateway node the user prefers; null means automatic selection. */
interface PreferredNodeStore {
    fun observe(): Flow<String?>

    suspend fun get(): String?

    suspend fun set(nodeId: String?)
}

class DataStorePreferredNodeStore(
    private val dataStore: SparrowDataStore
) : PreferredNodeStore {
    override fun observe(): Flow<String?> = dataStore.observeString(PREFERRED_NODE_KEY)

    override suspend fun get(): String? = dataStore.getString(PREFERRED_NODE_KEY)

    override suspend fun set(nodeId: String?) {
        dataStore.edit {
            if (nodeId == null) removeString(PREFERRED_NODE_KEY) else putString(PREFERRED_NODE_KEY, nodeId)
        }
    }

    private companion object {
        const val PREFERRED_NODE_KEY = "transport.preferred_node_id"
    }
}
