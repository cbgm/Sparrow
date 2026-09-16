package com.cbgm.sparrow.feature.invite.domain.usecase

class DeclineContactInvitationUseCase(
    private val declineInvitation: DeclineInvitationUseCase
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        declineInvitation(invitationId)
}
