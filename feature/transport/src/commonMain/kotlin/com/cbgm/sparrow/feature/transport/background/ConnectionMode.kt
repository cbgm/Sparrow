package com.cbgm.sparrow.feature.transport.background

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How messages reach this device while Sparrow is closed; Sparrow uses no push service. */
enum class ConnectionMode {
    /** A foreground service keeps the circuits open; messages arrive at once. */
    ALWAYS_CONNECTED,

    /** Messages wait in the mailboxes until the app is opened. */
    WHILE_OPEN,

    /** Android checks the mailboxes now and then (every 15 minutes at most). */
    CHECK_PERIODICALLY
}

interface ConnectionModeStore {
    fun observe(): Flow<ConnectionMode>

    suspend fun set(mode: ConnectionMode)
}

/** Until the user chooses, the app connects only while it is open, as before. */
class DataStoreConnectionModeStore(
    private val dataStore: SparrowDataStore
) : ConnectionModeStore {
    override fun observe(): Flow<ConnectionMode> =
        dataStore.observeString(MODE_KEY).map { stored -> ConnectionMode.entries.firstOrNull { it.name == stored } ?: ConnectionMode.WHILE_OPEN }

    override suspend fun set(mode: ConnectionMode) {
        dataStore.edit { putString(MODE_KEY, mode.name) }
    }

    private companion object {
        const val MODE_KEY = "transport.connection_mode"
    }
}

/** Whether this platform lets an app keep its own connection while it is closed: Android yes, iOS no. */
expect fun keepsConnectionWhileClosed(): Boolean
