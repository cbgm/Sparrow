package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.invite.domain.repository.DirectIdentityExchangeRepository

class AcceptDirectInvitationUseCase(
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository,
    private val acceptInvitation: AcceptInvitationUseCase,
    private val modeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        runCatching {
            check(modeRepository.getMode() == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                "Automatic identity invitations are disabled"
            }

            val contactId =
                directIdentityExchangeRepository
                    .getContactId(invitationId)
                    .getOrThrow()

            check(!contactBlocklistRepository.isBlocked(contactId)) {
                "Blocked contacts cannot be accepted"
            }

            acceptInvitation(invitationId).getOrThrow()
        }
}
