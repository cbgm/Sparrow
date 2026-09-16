package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicy
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow

class ObserveInvitationsUseCase(
    private val repository: InvitationRepository,
    private val policy: InvitationPolicy
) {
    operator fun invoke(direction: InvitationDirection): Flow<List<Invitation>> =
        policy.applyVisibility(
            direction = direction,
            invitations = repository.observeInvitations(direction)
        )
}
