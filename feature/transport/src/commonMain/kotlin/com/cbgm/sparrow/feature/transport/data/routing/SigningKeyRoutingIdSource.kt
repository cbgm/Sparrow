package com.cbgm.sparrow.feature.transport.data.routing

import com.cbgm.sparrow.feature.transport.domain.gateway.LocalRoutingIdSource
import com.cbgm.sparrow.feature.transport.domain.gateway.RoutingIdGenerator
import com.cbgm.sparrow.protocol.identity.LocalSigningPublicKeySource

internal class SigningKeyRoutingIdSource(
    private val localSigningPublicKeySource: LocalSigningPublicKeySource,
    private val routingIdGenerator: RoutingIdGenerator
) : LocalRoutingIdSource {
    override suspend fun getLocalRoutingId(): Result<String> =
        runCatching {
            val signingPublicKey =
                localSigningPublicKeySource.getSigningPublicKey().getOrThrow()

            routingIdGenerator
                .deriveFromSigningPublicKey(signingPublicKey = signingPublicKey)
                .getOrThrow()
        }
}
