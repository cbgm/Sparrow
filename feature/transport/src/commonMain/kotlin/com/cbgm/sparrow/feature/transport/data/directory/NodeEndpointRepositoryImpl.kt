package com.cbgm.sparrow.feature.transport.data.directory

import com.cbgm.sparrow.feature.transport.domain.model.NodeEndpoint
import com.cbgm.sparrow.feature.transport.domain.repository.NodeEndpointRepository

internal class NodeEndpointRepositoryImpl(
    private val remoteDataSource: NodeEndpointRemoteDataSource
) : NodeEndpointRepository {
    override suspend fun resolve(
        localRoutingId: String,
        forceRefresh: Boolean
    ): Result<List<NodeEndpoint>> = remoteDataSource.resolve(localRoutingId, forceRefresh)

    override suspend fun resolveCached(localRoutingId: String): Result<List<NodeEndpoint>?> = remoteDataSource.resolveCached(localRoutingId)
}
