package com.cbgm.sparrow.feature.transport.routing

import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider

class DefaultLocalBootstrapRoutingIdProvider(
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val routingIdGenerator: RoutingIdGenerator
) : LocalBootstrapRoutingIdProvider {
    override suspend fun getLocalBootstrapRoutingId(): Result<String> =
        runCatching {
            val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
            routingIdGenerator.deriveFromPhoneNumber(localPhoneNumber).getOrThrow()
        }
}
