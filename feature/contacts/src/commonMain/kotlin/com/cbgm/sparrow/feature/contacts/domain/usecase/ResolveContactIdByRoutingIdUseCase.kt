package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactTransportRepository

class ResolveContactIdByRoutingIdUseCase(
    private val repository: ContactTransportRepository
) {
    suspend operator fun invoke(routingId: String): String? = repository.resolveContactIdByRoutingId(routingId)
}
