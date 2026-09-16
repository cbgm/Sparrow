package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.repository.IdentityInvitationRepository

class DeclineContactInvitationUseCase(
    private val identityInvitationRepository: IdentityInvitationRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        identityInvitationRepository.decline(invitationId)
}
