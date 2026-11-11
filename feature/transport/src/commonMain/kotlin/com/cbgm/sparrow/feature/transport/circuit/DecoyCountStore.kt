package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * How many decoy envelopes go out with each send, [DecoyCount.MINIMUM] (off, the default) to [DecoyCount.MAXIMUM]:
 * they go into other contacts' mailboxes at the same moment, so a mailbox node cannot tell from timing which mailboxes
 * a send was meant for.
 */
interface DecoyCountStore {
    fun observe(): Flow<Int>

    suspend fun get(): Int = observe().first()

    suspend fun set(decoyCount: Int)
}

object DecoyCount {
    const val MINIMUM = 0
    const val MAXIMUM = 5
    const val DEFAULT = 0
}

class DataStoreDecoyCountStore(
    private val dataStore: SparrowDataStore
) : DecoyCountStore {
    override fun observe(): Flow<Int> =
        dataStore
            .observeLong(DECOY_COUNT_KEY, DecoyCount.DEFAULT.toLong())
            .map { stored -> stored.toInt().coerceIn(DecoyCount.MINIMUM, DecoyCount.MAXIMUM) }

    override suspend fun set(decoyCount: Int) {
        dataStore.edit {
            putLong(DECOY_COUNT_KEY, decoyCount.coerceIn(DecoyCount.MINIMUM, DecoyCount.MAXIMUM).toLong())
        }
    }

    private companion object {
        const val DECOY_COUNT_KEY = "transport.decoy_count"
    }
}
