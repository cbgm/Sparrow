package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.feature.invite.domain.repository.DirectInvitationRepository

class DeclineAndBlockDirectInvitationUseCase(
    private val directInvitationRepository: DirectInvitationRepository,
    private val declineInvitation: DeclineInvitationUseCase,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        runCatching {
            val contactId =
                directInvitationRepository
                    .getContactId(invitationId)
                    .getOrThrow()

            contactBlocklistRepository.block(contactId)
            declineInvitation(invitationId).getOrThrow()
        }
}
