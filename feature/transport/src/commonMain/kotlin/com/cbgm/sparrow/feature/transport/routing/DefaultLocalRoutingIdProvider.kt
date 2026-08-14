package com.cbgm.sparrow.feature.transport.routing

import com.cbgm.sparrow.core.protocol.identity.LocalSigningPublicKeyProvider

class DefaultLocalRoutingIdProvider(
    private val localSigningPublicKeyProvider: LocalSigningPublicKeyProvider,
    private val routingIdGenerator: RoutingIdGenerator
) : LocalRoutingIdProvider {
    override suspend fun getLocalRoutingId(): Result<String> =
        runCatching {
            val signingPublicKey =
                localSigningPublicKeyProvider.getSigningPublicKey().getOrThrow()

            routingIdGenerator
                .deriveFromSigningPublicKey(signingPublicKey = signingPublicKey)
                .getOrThrow()
        }
}
