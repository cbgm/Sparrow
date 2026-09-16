package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicyProvider
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository

class DeclineAndBlockInvitationUseCase(
    private val repository: InvitationRepository,
    private val declineInvitation: DeclineInvitationUseCase,
    private val policyProvider: InvitationPolicyProvider
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        runCatching {
            val peerId = repository.getPeerId(invitationId).getOrThrow()
            policyProvider.blockPeer(peerId)
            declineInvitation(invitationId).getOrThrow()
        }
}
