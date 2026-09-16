package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.repository.DirectInvitationRepository

class DeleteDeclinedOutgoingInvitationUseCase(
    private val repository: DirectInvitationRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        repository.deleteDeclinedOutgoing(invitationId)
}
