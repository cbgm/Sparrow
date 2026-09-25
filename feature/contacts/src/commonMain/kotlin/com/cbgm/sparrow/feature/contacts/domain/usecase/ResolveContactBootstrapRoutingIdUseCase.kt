package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactTransportRepository

class ResolveContactBootstrapRoutingIdUseCase(
    private val repository: ContactTransportRepository
) {
    suspend operator fun invoke(contactId: String): String = repository.resolveBootstrapRoutingId(contactId)
}
