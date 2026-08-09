package com.cbgm.securechat.feature.transport.discovery

interface NodeEndpointResolver {
    suspend fun resolve(
        localRelayId: String,
        forceRefresh: Boolean = false
    ): Result<List<NodeEndpoint>>
}
