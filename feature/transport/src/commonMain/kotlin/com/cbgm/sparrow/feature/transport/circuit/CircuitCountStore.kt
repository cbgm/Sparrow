package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * How many circuits the app keeps open at once, [CircuitCount.MINIMUM] to [CircuitCount.MAXIMUM]. Contacts are spread
 * across them, so one session node does not see all of the device's mailboxes together.
 */
interface CircuitCountStore {
    fun observe(): Flow<Int>

    suspend fun set(circuitCount: Int)
}

object CircuitCount {
    const val MINIMUM = 1
    const val MAXIMUM = 3
    const val DEFAULT = 2
}

class DataStoreCircuitCountStore(
    private val dataStore: SparrowDataStore
) : CircuitCountStore {
    override fun observe(): Flow<Int> =
        dataStore
            .observeLong(CIRCUIT_COUNT_KEY, CircuitCount.DEFAULT.toLong())
            .map { stored -> stored.toInt().coerceIn(CircuitCount.MINIMUM, CircuitCount.MAXIMUM) }

    override suspend fun set(circuitCount: Int) {
        dataStore.edit {
            putLong(CIRCUIT_COUNT_KEY, circuitCount.coerceIn(CircuitCount.MINIMUM, CircuitCount.MAXIMUM).toLong())
        }
    }

    private companion object {
        const val CIRCUIT_COUNT_KEY = "transport.circuit_count"
    }
}
