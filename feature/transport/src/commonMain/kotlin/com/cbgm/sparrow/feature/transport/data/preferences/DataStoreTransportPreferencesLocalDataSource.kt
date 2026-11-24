package com.cbgm.sparrow.feature.transport.data.preferences

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.transport.domain.model.CircuitCount
import com.cbgm.sparrow.feature.transport.domain.model.CircuitHopCount
import com.cbgm.sparrow.feature.transport.domain.model.ConnectionMode
import com.cbgm.sparrow.feature.transport.domain.model.DecoyCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DataStoreTransportPreferencesLocalDataSource(
    private val dataStore: SparrowDataStore
) : TransportPreferencesLocalDataSource {
    override fun observeCircuitCount(): Flow<Int> = observeCount(CIRCUIT_COUNT_KEY, CircuitCount.DEFAULT, CircuitCount.MINIMUM..CircuitCount.MAXIMUM)

    override suspend fun setCircuitCount(circuitCount: Int) = setCount(CIRCUIT_COUNT_KEY, circuitCount, CircuitCount.MINIMUM..CircuitCount.MAXIMUM)

    override fun observeHopCount(): Flow<Int> = observeCount(HOP_COUNT_KEY, CircuitHopCount.DEFAULT, CircuitHopCount.MINIMUM..CircuitHopCount.MAXIMUM)

    override suspend fun setHopCount(hopCount: Int) = setCount(HOP_COUNT_KEY, hopCount, CircuitHopCount.MINIMUM..CircuitHopCount.MAXIMUM)

    override fun observeDecoyCount(): Flow<Int> = observeCount(DECOY_COUNT_KEY, DecoyCount.DEFAULT, DecoyCount.MINIMUM..DecoyCount.MAXIMUM)

    override suspend fun setDecoyCount(decoyCount: Int) = setCount(DECOY_COUNT_KEY, decoyCount, DecoyCount.MINIMUM..DecoyCount.MAXIMUM)

    override fun observeSendsDirectly(): Flow<Boolean> = dataStore.observeBoolean(SENDS_DIRECTLY_KEY, defaultValue = true)

    override suspend fun setSendsDirectly(sendsDirectly: Boolean) {
        dataStore.edit { putBoolean(SENDS_DIRECTLY_KEY, sendsDirectly) }
    }

    override fun observePreferredNode(): Flow<String?> = dataStore.observeString(PREFERRED_NODE_KEY)

    override suspend fun setPreferredNode(nodeId: String?) {
        dataStore.edit {
            if (nodeId == null) removeString(PREFERRED_NODE_KEY) else putString(PREFERRED_NODE_KEY, nodeId)
        }
    }

    override fun observeConnectionMode(): Flow<ConnectionMode> =
        dataStore.observeString(CONNECTION_MODE_KEY).map { stored -> ConnectionMode.entries.firstOrNull { it.name == stored } ?: ConnectionMode.WHILE_OPEN }

    override suspend fun setConnectionMode(mode: ConnectionMode) {
        dataStore.edit { putString(CONNECTION_MODE_KEY, mode.name) }
    }

    private fun observeCount(
        key: String,
        default: Int,
        bounds: IntRange
    ): Flow<Int> = dataStore.observeLong(key, default.toLong()).map { stored -> stored.toInt().coerceIn(bounds) }

    private suspend fun setCount(
        key: String,
        count: Int,
        bounds: IntRange
    ) {
        dataStore.edit { putLong(key, count.coerceIn(bounds).toLong()) }
    }

    private companion object {
        const val CIRCUIT_COUNT_KEY = "transport.circuit_count"
        const val HOP_COUNT_KEY = "transport.circuit_hop_count"
        const val DECOY_COUNT_KEY = "transport.decoy_count"
        const val SENDS_DIRECTLY_KEY = "transport.circuit_sends_directly_without_circuit"
        const val PREFERRED_NODE_KEY = "transport.preferred_node_id"
        const val CONNECTION_MODE_KEY = "transport.connection_mode"
    }
}
