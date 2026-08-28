package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository

class DeleteDeclinedOutgoingInvitationUseCase(
    private val repository: IdentityInvitationRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        repository.deleteDeclinedOutgoing(invitationId)
}
