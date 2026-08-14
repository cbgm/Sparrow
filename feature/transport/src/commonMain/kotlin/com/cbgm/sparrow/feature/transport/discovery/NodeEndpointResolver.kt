package com.cbgm.sparrow.feature.transport.discovery

interface NodeEndpointResolver {
    suspend fun resolve(
        localRoutingId: String,
        forceRefresh: Boolean = false
    ): Result<List<NodeEndpoint>>
}
