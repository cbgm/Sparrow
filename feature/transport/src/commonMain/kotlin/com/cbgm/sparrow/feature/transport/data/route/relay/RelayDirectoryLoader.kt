package com.cbgm.sparrow.feature.transport.data.route.relay

import com.cbgm.sparrow.feature.transport.data.directory.NodeEndpointRemoteDataSource
import com.cbgm.sparrow.feature.transport.data.route.RelaySelector

/** Makes sure relays are known before a request that may only go through them, also before the first connection. */
internal interface RelayDirectoryLoader {
    suspend fun ensureRelays()
}

/**
 * Before the first connection (onboarding) no node directory is cached, so no relay is known: the signed node
 * directory is fetched then, the same way the connection would fetch it.
 */
internal class NodeDirectoryRelayLoader(
    private val selector: RelaySelector,
    private val nodeEndpoints: NodeEndpointRemoteDataSource
) : RelayDirectoryLoader {
    override suspend fun ensureRelays() {
        if (selector.route().isNotEmpty()) return
        nodeEndpoints.resolve(localRoutingId = ORDERING_ROUTING_ID).getOrThrow()
    }

    private companion object {
        // Only orders the gateways of the directory; no route or mailbox uses it.
        const val ORDERING_ROUTING_ID = "scrouting1_onboarding"
    }
}
