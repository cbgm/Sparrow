package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationStatus
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObservePendingContactInvitationsUseCase(
    private val invitationRepository: InvitationRepository,
    private val modeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    operator fun invoke(): Flow<List<Invitation>> =
        combine(
            invitationRepository.observeInvitations(InvitationDirection.INCOMING),
            modeRepository.observeMode(),
            contactBlocklistRepository.observeBlockedContactIds()
        ) { invitations, mode, blockedContactIds ->
            if (mode == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                invitations.filter { invitation ->
                    invitation.status == InvitationStatus.PENDING &&
                        invitation.peerId !in blockedContactIds
                }
            } else {
                emptyList()
            }
        }
}
