package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.invite.domain.model.PendingContactInvitation
import com.cbgm.sparrow.feature.invite.domain.repository.DirectInvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObservePendingContactInvitationsUseCase(
    private val identityInvitationRepository: DirectInvitationRepository,
    private val modeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    operator fun invoke(): Flow<List<PendingContactInvitation>> =
        combine(
            identityInvitationRepository.observePendingIncoming(),
            modeRepository.observeMode(),
            contactBlocklistRepository.observeBlockedContactIds()
        ) { invitations, mode, blockedContactIds ->
            if (mode == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                invitations.filterNot { invitation -> invitation.contactId in blockedContactIds }
            } else {
                emptyList()
            }
        }
}
