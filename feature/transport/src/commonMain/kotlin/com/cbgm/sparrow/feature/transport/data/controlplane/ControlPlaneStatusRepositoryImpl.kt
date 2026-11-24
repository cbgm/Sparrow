package com.cbgm.sparrow.feature.transport.data.controlplane

import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpoint
import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpointStatus
import com.cbgm.sparrow.feature.transport.domain.repository.ControlPlaneStatusRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow

internal class ControlPlaneStatusRepositoryImpl(
    private val localDataSource: ControlPlaneStatusLocalDataSource,
    private val healthRemoteDataSource: ControlPlaneHealthRemoteDataSource
) : ControlPlaneStatusRepository {
    override val statuses: StateFlow<List<ControlPlaneEndpointStatus>> = localDataSource.statuses

    override fun markAvailable(endpoint: ControlPlaneEndpoint) = localDataSource.markAvailable(endpoint)

    override fun markUnreachable(endpoint: ControlPlaneEndpoint) = localDataSource.markUnreachable(endpoint)

    override suspend fun refreshHealth() {
        coroutineScope {
            localDataSource.endpoints.value.map { endpoint -> async { probe(endpoint) } }.awaitAll()
        }
    }

    private suspend fun probe(endpoint: ControlPlaneEndpoint) {
        if (healthRemoteDataSource.isAvailable(endpoint)) markAvailable(endpoint) else markUnreachable(endpoint)
    }
}
