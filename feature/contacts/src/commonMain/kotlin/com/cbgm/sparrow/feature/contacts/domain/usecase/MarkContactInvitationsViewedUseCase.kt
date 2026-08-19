package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.IdentityInvitationDirection
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository

class MarkContactInvitationsViewedUseCase(
    private val repository: IdentityInvitationRepository
) {
    suspend operator fun invoke(direction: IdentityInvitationDirection): Result<Unit> =
        repository.markViewed(direction)
}
