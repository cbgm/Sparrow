package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository

class DeclineAndBlockInvitationUseCase(
    private val repository: InvitationRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        repository.decline(
            invitationId = invitationId,
            action = InvitationResultAction.BLOCK_PEER
        )
}
