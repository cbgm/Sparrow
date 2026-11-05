package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.discovery.GatewayNode
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.discovery.toGatewayNode
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider

/**
 * The gateways of the verified node directory in connection order: the preferred node first, then by
 * load. Uses the saved directory when it is still valid and fetches a fresh one otherwise.
 */
class GetGatewayNodesUseCase(
    private val nodeEndpointResolver: NodeEndpointResolver,
    private val localRoutingIdProvider: LocalRoutingIdProvider
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<List<GatewayNode>> =
        safeSuspendCall {
            val routingId = localRoutingIdProvider.getLocalRoutingId().getOrThrow()
            val cached =
                nodeEndpointResolver
                    .resolveCached(routingId)
                    .getOrNull()
                    ?.takeUnless { forceRefresh }
            (cached ?: nodeEndpointResolver.resolve(routingId, forceRefresh = forceRefresh).getOrThrow())
                .map { endpoint -> endpoint.toGatewayNode() }
        }
}
