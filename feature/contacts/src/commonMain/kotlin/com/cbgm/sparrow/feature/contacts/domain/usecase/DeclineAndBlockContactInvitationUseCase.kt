package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository

class DeclineAndBlockContactInvitationUseCase(
    private val identityInvitationRepository: IdentityInvitationRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        identityInvitationRepository.declineAndBlock(invitationId)
}
