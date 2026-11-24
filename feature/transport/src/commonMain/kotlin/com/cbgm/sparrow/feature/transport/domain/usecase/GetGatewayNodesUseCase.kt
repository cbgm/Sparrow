package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.domain.model.GatewayNode
import com.cbgm.sparrow.feature.transport.domain.model.toGatewayNode
import com.cbgm.sparrow.feature.transport.domain.repository.NodeEndpointRepository
import com.cbgm.sparrow.feature.transport.domain.repository.RoutingIdRepository

/**
 * The gateways of the verified node directory in connection order: the preferred node first, then by
 * load. Uses the saved directory when it is still valid and fetches a fresh one otherwise.
 */
class GetGatewayNodesUseCase(
    private val nodeEndpointResolver: NodeEndpointRepository,
    private val routingIdRepository: RoutingIdRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<List<GatewayNode>> =
        safeSuspendCall {
            val routingId = routingIdRepository.getLocalRoutingId().getOrThrow()
            val cached =
                nodeEndpointResolver
                    .resolveCached(routingId)
                    .getOrNull()
                    ?.takeUnless { forceRefresh }
            (cached ?: nodeEndpointResolver.resolve(routingId, forceRefresh = forceRefresh).getOrThrow())
                .map { endpoint -> endpoint.toGatewayNode() }
        }
}
