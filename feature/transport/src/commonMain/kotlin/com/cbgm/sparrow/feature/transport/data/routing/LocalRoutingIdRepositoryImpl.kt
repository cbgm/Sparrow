package com.cbgm.sparrow.feature.transport.data.routing

import com.cbgm.sparrow.feature.transport.domain.repository.LocalRoutingIdRepository
import com.cbgm.sparrow.feature.transport.domain.rule.RoutingIdGenerator
import com.cbgm.sparrow.protocol.identity.LocalSigningPublicKeySource

internal class LocalRoutingIdRepositoryImpl(
    private val localSigningPublicKeySource: LocalSigningPublicKeySource,
    private val routingIdGenerator: RoutingIdGenerator
) : LocalRoutingIdRepository {
    override suspend fun getLocalRoutingId(): Result<String> =
        runCatching {
            val signingPublicKey =
                localSigningPublicKeySource.getSigningPublicKey().getOrThrow()

            routingIdGenerator
                .deriveFromSigningPublicKey(signingPublicKey = signingPublicKey)
                .getOrThrow()
        }
}
