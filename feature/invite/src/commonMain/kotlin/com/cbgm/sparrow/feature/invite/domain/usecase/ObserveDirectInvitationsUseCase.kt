package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveDirectInvitationsUseCase(
    private val observeInvitations: ObserveInvitationsUseCase,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    operator fun invoke(direction: InvitationDirection): Flow<List<Invitation>> =
        combine(
            observeInvitations(direction),
            contactBlocklistRepository.observeBlockedContactIds()
        ) { invitations, blockedContactIds ->
            if (direction == InvitationDirection.INCOMING) {
                invitations.filterNot { invitation -> invitation.peerId in blockedContactIds }
            } else {
                invitations
            }
        }
}
