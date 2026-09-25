package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObservePendingInvitationsUseCase(
    private val observeInvitations: ObserveInvitationsUseCase
) {
    operator fun invoke(): Flow<List<Invitation>> =
        observeInvitations(InvitationDirection.INCOMING)
            .map { invitations ->
                invitations.filter { invitation -> invitation.status == InvitationStatus.PENDING }
            }
}
