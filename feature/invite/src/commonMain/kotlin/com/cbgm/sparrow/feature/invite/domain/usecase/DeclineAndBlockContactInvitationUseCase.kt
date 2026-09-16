package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.feature.invite.domain.repository.DirectInvitationRepository

class DeclineAndBlockContactInvitationUseCase(
    private val identityInvitationRepository: DirectInvitationRepository,
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
