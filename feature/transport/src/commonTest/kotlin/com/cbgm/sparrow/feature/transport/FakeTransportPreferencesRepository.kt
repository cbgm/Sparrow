package com.cbgm.sparrow.feature.transport

import com.cbgm.sparrow.feature.transport.data.preferences.TransportPreferencesLocalDataSource
import com.cbgm.sparrow.feature.transport.domain.model.ConnectionMode
import com.cbgm.sparrow.feature.transport.domain.repository.TransportPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Choices held in memory, for tests. */
internal class FakeTransportPreferencesRepository(
    circuitCount: Int = 2,
    hopCount: Int = 2,
    decoyCount: Int = 0,
    sendsDirectly: Boolean = true,
    preferredNode: String? = null,
    connectionMode: ConnectionMode = ConnectionMode.WHILE_OPEN
) : TransportPreferencesLocalDataSource,
    TransportPreferencesRepository {
    private val circuitCount = MutableStateFlow(circuitCount)
    private val hopCount = MutableStateFlow(hopCount)
    private val decoyCount = MutableStateFlow(decoyCount)
    private val sendsDirectly = MutableStateFlow(sendsDirectly)
    private val preferredNode = MutableStateFlow(preferredNode)
    private val connectionMode = MutableStateFlow(connectionMode)

    override fun observeCircuitCount(): Flow<Int> = circuitCount

    override suspend fun setCircuitCount(circuitCount: Int) {
        this.circuitCount.value = circuitCount
    }

    override fun observeHopCount(): Flow<Int> = hopCount

    override suspend fun setHopCount(hopCount: Int) {
        this.hopCount.value = hopCount
    }

    override fun observeDecoyCount(): Flow<Int> = decoyCount

    override suspend fun setDecoyCount(decoyCount: Int) {
        this.decoyCount.value = decoyCount
    }

    override fun observeSendsDirectly(): Flow<Boolean> = sendsDirectly

    override suspend fun setSendsDirectly(sendsDirectly: Boolean) {
        this.sendsDirectly.value = sendsDirectly
    }

    override fun observePreferredNode(): Flow<String?> = preferredNode

    override suspend fun setPreferredNode(nodeId: String?) {
        preferredNode.value = nodeId
    }

    override fun observeConnectionMode(): Flow<ConnectionMode> = connectionMode

    override suspend fun setConnectionMode(mode: ConnectionMode) {
        connectionMode.value = mode
    }
}
