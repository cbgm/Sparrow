package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.domain.gateway.LocalRoutingIdSource
import com.cbgm.sparrow.feature.transport.domain.gateway.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.domain.model.GatewayNode
import com.cbgm.sparrow.feature.transport.domain.model.toGatewayNode

/**
 * The gateways of the verified node directory in connection order: the preferred node first, then by
 * load. Uses the saved directory when it is still valid and fetches a fresh one otherwise.
 */
class GetGatewayNodesUseCase(
    private val nodeEndpointResolver: NodeEndpointResolver,
    private val localRoutingIdSource: LocalRoutingIdSource
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<List<GatewayNode>> =
        safeSuspendCall {
            val routingId = localRoutingIdSource.getLocalRoutingId().getOrThrow()
            val cached =
                nodeEndpointResolver
                    .resolveCached(routingId)
                    .getOrNull()
                    ?.takeUnless { forceRefresh }
            (cached ?: nodeEndpointResolver.resolve(routingId, forceRefresh = forceRefresh).getOrThrow())
                .map { endpoint -> endpoint.toGatewayNode() }
        }
}
