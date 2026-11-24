package com.cbgm.sparrow.feature.transport.data.routing

import com.cbgm.sparrow.core.util.phone.LocalPhoneNumberSource
import com.cbgm.sparrow.feature.transport.domain.repository.LocalBootstrapRoutingIdRepository
import com.cbgm.sparrow.feature.transport.domain.rule.RoutingIdGenerator

internal class LocalBootstrapRoutingIdRepositoryImpl(
    private val localPhoneNumberSource: LocalPhoneNumberSource,
    private val routingIdGenerator: RoutingIdGenerator
) : LocalBootstrapRoutingIdRepository {
    override suspend fun getLocalBootstrapRoutingId(): Result<String> =
        runCatching {
            val localPhoneNumber = localPhoneNumberSource.getLocalPhoneNumber().getOrThrow()
            routingIdGenerator.deriveFromPhoneNumber(localPhoneNumber).getOrThrow()
        }
}
