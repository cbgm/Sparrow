package com.cbgm.sparrow.feature.transport.discovery

interface NodeEndpointResolver {
    suspend fun resolve(
        localRoutingId: String,
        forceRefresh: Boolean = false
    ): Result<List<NodeEndpoint>>

    // Endpoints from the cached signed directory without any network request, or null when no
    // usable cache exists. Used to connect immediately while a fresh directory is fetched.
    suspend fun resolveCached(localRoutingId: String): Result<List<NodeEndpoint>?> = Result.success(null)
}
