package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository

class DeclineAndBlockContactInvitationUseCase(
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        runCatching {
            val contactId =
                identityInvitationRepository
                    .getContactId(invitationId)
                    .getOrThrow()

            contactBlocklistRepository.block(contactId)
            identityInvitationRepository.decline(invitationId).getOrThrow()
        }
}
