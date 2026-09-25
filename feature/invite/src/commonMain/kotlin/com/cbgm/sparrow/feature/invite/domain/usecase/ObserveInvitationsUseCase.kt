package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow

class ObserveInvitationsUseCase(
    private val repository: InvitationRepository
) {
    operator fun invoke(direction: InvitationDirection): Flow<List<Invitation>> =
        repository.observeInvitations(direction)
}
