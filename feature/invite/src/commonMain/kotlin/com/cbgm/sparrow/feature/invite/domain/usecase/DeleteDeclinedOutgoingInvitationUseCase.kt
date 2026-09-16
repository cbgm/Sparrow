package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.repository.IdentityInvitationRepository

class DeleteDeclinedOutgoingInvitationUseCase(
    private val repository: IdentityInvitationRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        repository.deleteDeclinedOutgoing(invitationId)
}
