package com.cbgm.sparrow.feature.transport.data.routing

import com.cbgm.sparrow.core.util.phone.LocalPhoneNumberSource
import com.cbgm.sparrow.feature.transport.domain.repository.RoutingIdRepository
import com.cbgm.sparrow.feature.transport.domain.rule.RoutingIdGenerator
import com.cbgm.sparrow.protocol.identity.LocalSigningPublicKeySource

internal class RoutingIdRepositoryImpl(
    private val localSigningPublicKeySource: LocalSigningPublicKeySource,
    private val localPhoneNumberSource: LocalPhoneNumberSource,
    private val routingIdGenerator: RoutingIdGenerator
) : RoutingIdRepository {
    override suspend fun getLocalRoutingId(): Result<String> =
        runCatching {
            val signingPublicKey =
                localSigningPublicKeySource.getSigningPublicKey().getOrThrow()

            routingIdGenerator
                .deriveFromSigningPublicKey(signingPublicKey = signingPublicKey)
                .getOrThrow()
        }

    override suspend fun getLocalBootstrapRoutingId(): Result<String> =
        runCatching {
            val localPhoneNumber = localPhoneNumberSource.getLocalPhoneNumber().getOrThrow()
            routingIdGenerator.deriveFromPhoneNumber(localPhoneNumber).getOrThrow()
        }
}
