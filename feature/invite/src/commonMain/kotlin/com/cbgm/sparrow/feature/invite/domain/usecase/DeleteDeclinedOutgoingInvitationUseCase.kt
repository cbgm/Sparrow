package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository

class DeleteDeclinedOutgoingInvitationUseCase(
    private val repository: InvitationRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        repository.deleteDeclinedOutgoing(invitationId)
}
