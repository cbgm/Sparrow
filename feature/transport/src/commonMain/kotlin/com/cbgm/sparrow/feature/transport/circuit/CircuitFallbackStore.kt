package com.cbgm.sparrow.feature.transport.circuit

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Whether a request that should go through the circuit is sent directly while there is none (the default: messages
 * keep working, but that server sees the device's address), or waits for one.
 */
interface CircuitFallbackStore {
    fun observeSendsDirectly(): Flow<Boolean>

    suspend fun sendsDirectly(): Boolean = observeSendsDirectly().first()

    suspend fun setSendsDirectly(sendsDirectly: Boolean)
}

class DataStoreCircuitFallbackStore(
    private val dataStore: SparrowDataStore
) : CircuitFallbackStore {
    override fun observeSendsDirectly(): Flow<Boolean> = dataStore.observeBoolean(SENDS_DIRECTLY_KEY, defaultValue = true)

    override suspend fun setSendsDirectly(sendsDirectly: Boolean) {
        dataStore.edit { putBoolean(SENDS_DIRECTLY_KEY, sendsDirectly) }
    }

    private companion object {
        const val SENDS_DIRECTLY_KEY = "transport.circuit_sends_directly_without_circuit"
    }
}
