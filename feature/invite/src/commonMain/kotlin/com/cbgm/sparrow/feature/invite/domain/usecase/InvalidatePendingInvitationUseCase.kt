package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository

/** Dismiss a no-longer-valid pending invitation without sending an acceptance or decline. */
class InvalidatePendingInvitationUseCase(
    private val repository: InvitationRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        repository.invalidatePending(invitationId)
}
