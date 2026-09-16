package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository

class MarkInvitationsViewedUseCase(
    private val repository: InvitationRepository
) {
    suspend operator fun invoke(direction: InvitationDirection): Result<Unit> =
        repository.markViewed(direction)
}
