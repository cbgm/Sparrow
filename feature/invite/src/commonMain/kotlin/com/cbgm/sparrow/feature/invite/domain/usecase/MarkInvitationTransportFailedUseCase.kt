package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository

class MarkInvitationTransportFailedUseCase(
    private val repository: InvitationRepository
) {
    suspend operator fun invoke(
        payloadType: InvitationPayloadType,
        invitationId: String
    ): Result<Unit> =
        repository.markTransportFailed(
            payloadType = payloadType,
            invitationId = invitationId
        )
}
