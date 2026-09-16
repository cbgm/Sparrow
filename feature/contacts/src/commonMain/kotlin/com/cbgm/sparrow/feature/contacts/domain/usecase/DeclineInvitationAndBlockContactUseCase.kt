package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.feature.invite.domain.repository.DirectIdentityExchangeRepository
import com.cbgm.sparrow.feature.invite.domain.usecase.DeclineInvitationUseCase

class DeclineInvitationAndBlockContactUseCase(
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository,
    private val declineInvitation: DeclineInvitationUseCase,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        runCatching {
            val contactId =
                directIdentityExchangeRepository
                    .getContactId(invitationId)
                    .getOrThrow()

            contactBlocklistRepository.block(contactId)
            declineInvitation(invitationId).getOrThrow()
        }
}
