package com.cbgm.sparrow.feature.transport.data.routing

import com.cbgm.sparrow.feature.transport.domain.repository.RoutingIdRepository

internal class RoutingIdRepositoryImpl(
    private val localDataSource: RoutingIdLocalDataSource
) : RoutingIdRepository {
    override suspend fun getLocalRoutingId(): Result<String> = localDataSource.getLocalRoutingId()

    override suspend fun getLocalBootstrapRoutingId(): Result<String> = localDataSource.getLocalBootstrapRoutingId()
}
