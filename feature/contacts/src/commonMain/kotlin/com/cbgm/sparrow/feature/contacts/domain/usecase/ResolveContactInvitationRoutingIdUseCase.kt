package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactTransportRepository

/** Direct invitation traffic only. Never use this for encrypted messages or group routing. */
class ResolveContactInvitationRoutingIdUseCase(
    private val repository: ContactTransportRepository
) {
    suspend operator fun invoke(contactId: String): String = repository.resolveInvitationRoutingId(contactId)
}
