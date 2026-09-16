package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationReceptionPolicy
import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicy

class HandleIncomingInvitationUseCase(
    private val policy: InvitationPolicy
) {
    suspend operator fun invoke(
        receiveInvitation: suspend (InvitationReceptionPolicy) -> Result<Unit>
    ): Result<Unit> =
        receiveInvitation(policy.getReceptionPolicy())
}
