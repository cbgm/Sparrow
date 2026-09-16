package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.IdentityInvitationDirection
import com.cbgm.sparrow.feature.invite.domain.repository.DirectInvitationRepository

class MarkContactInvitationsViewedUseCase(
    private val repository: DirectInvitationRepository
) {
    suspend operator fun invoke(direction: IdentityInvitationDirection): Result<Unit> =
        repository.markViewed(direction)
}
