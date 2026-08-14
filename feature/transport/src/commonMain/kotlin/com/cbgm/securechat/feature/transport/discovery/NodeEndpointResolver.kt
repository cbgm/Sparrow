package com.cbgm.securechat.feature.transport.discovery

interface NodeEndpointResolver {
    suspend fun resolve(
        localRoutingId: String,
        forceRefresh: Boolean = false
    ): Result<List<NodeEndpoint>>
}
