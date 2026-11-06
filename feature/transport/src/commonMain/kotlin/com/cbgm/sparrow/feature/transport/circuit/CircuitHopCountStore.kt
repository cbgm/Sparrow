package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How many hops the user wants their circuits to have, [CircuitHopCount.MINIMUM] to [CircuitHopCount.MAXIMUM]. */
interface CircuitHopCountStore {
    fun observe(): Flow<Int>

    suspend fun set(hopCount: Int)
}

object CircuitHopCount {
    const val MINIMUM = 1
    const val MAXIMUM = 5
    const val DEFAULT = 2
}

class DataStoreCircuitHopCountStore(
    private val dataStore: SparrowDataStore
) : CircuitHopCountStore {
    override fun observe(): Flow<Int> =
        dataStore
            .observeLong(HOP_COUNT_KEY, CircuitHopCount.DEFAULT.toLong())
            .map { stored -> stored.toInt().coerceIn(CircuitHopCount.MINIMUM, CircuitHopCount.MAXIMUM) }

    override suspend fun set(hopCount: Int) {
        dataStore.edit {
            putLong(HOP_COUNT_KEY, hopCount.coerceIn(CircuitHopCount.MINIMUM, CircuitHopCount.MAXIMUM).toLong())
        }
    }

    private companion object {
        const val HOP_COUNT_KEY = "transport.circuit_hop_count"
    }
}
