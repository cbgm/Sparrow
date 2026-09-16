package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicy
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository

class AcceptInvitationUseCase(
    private val repository: InvitationRepository,
    private val policy: InvitationPolicy
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        runCatching {
            policy.validateAcceptance(invitationId).getOrThrow()
            repository.accept(invitationId).getOrThrow()
        }
}
