package com.cbgm.sparrow.feature.transport.data.preferences

import com.cbgm.sparrow.feature.transport.domain.model.ConnectionMode
import com.cbgm.sparrow.feature.transport.domain.repository.TransportPreferencesRepository
import kotlinx.coroutines.flow.Flow

internal class TransportPreferencesRepositoryImpl(
    private val localDataSource: TransportPreferencesLocalDataSource
) : TransportPreferencesRepository {
    override fun observeCircuitCount(): Flow<Int> = localDataSource.observeCircuitCount()

    override suspend fun setCircuitCount(circuitCount: Int) = localDataSource.setCircuitCount(circuitCount)

    override fun observeHopCount(): Flow<Int> = localDataSource.observeHopCount()

    override suspend fun setHopCount(hopCount: Int) = localDataSource.setHopCount(hopCount)

    override fun observeDecoyCount(): Flow<Int> = localDataSource.observeDecoyCount()

    override suspend fun setDecoyCount(decoyCount: Int) = localDataSource.setDecoyCount(decoyCount)

    override fun observeSendsDirectly(): Flow<Boolean> = localDataSource.observeSendsDirectly()

    override suspend fun setSendsDirectly(sendsDirectly: Boolean) = localDataSource.setSendsDirectly(sendsDirectly)

    override fun observePreferredNode(): Flow<String?> = localDataSource.observePreferredNode()

    override suspend fun setPreferredNode(nodeId: String?) = localDataSource.setPreferredNode(nodeId)

    override fun observeConnectionMode(): Flow<ConnectionMode> = localDataSource.observeConnectionMode()

    override suspend fun setConnectionMode(mode: ConnectionMode) = localDataSource.setConnectionMode(mode)
}
