package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.repository.DirectInvitationRepository

class DeclineContactInvitationUseCase(
    private val identityInvitationRepository: DirectInvitationRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        identityInvitationRepository.decline(invitationId)
}
