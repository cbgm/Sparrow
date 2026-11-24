package com.cbgm.sparrow.feature.transport.data.routing

import com.cbgm.sparrow.core.util.phone.LocalPhoneNumberSource
import com.cbgm.sparrow.feature.transport.domain.gateway.LocalBootstrapRoutingIdSource
import com.cbgm.sparrow.feature.transport.domain.gateway.RoutingIdGenerator

internal class PhoneNumberRoutingIdSource(
    private val localPhoneNumberSource: LocalPhoneNumberSource,
    private val routingIdGenerator: RoutingIdGenerator
) : LocalBootstrapRoutingIdSource {
    override suspend fun getLocalBootstrapRoutingId(): Result<String> =
        runCatching {
            val localPhoneNumber = localPhoneNumberSource.getLocalPhoneNumber().getOrThrow()
            routingIdGenerator.deriveFromPhoneNumber(localPhoneNumber).getOrThrow()
        }
}
