package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository

class ValidatePendingInvitationUseCase(
    private val repository: InvitationRepository
) {
    suspend operator fun invoke(
        payloadType: InvitationPayloadType,
        invitationId: String,
        payloadId: String,
        peerId: String,
        direction: InvitationDirection,
        atEpochMilliseconds: Long
    ): Result<Unit> =
        repository.validatePending(
            payloadType = payloadType,
            invitationId = invitationId,
            payloadId = payloadId,
            peerId = peerId,
            direction = direction,
            atEpochMilliseconds = atEpochMilliseconds
        )
}
