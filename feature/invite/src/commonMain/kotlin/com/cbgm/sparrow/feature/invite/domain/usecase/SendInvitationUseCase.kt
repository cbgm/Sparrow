package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository

class SendInvitationUseCase(
    private val repository: InvitationRepository
) {
    suspend operator fun invoke(
        payloadType: InvitationPayloadType,
        payloadId: String,
        peerIds: Set<String>
    ): Result<Unit> =
        repository.send(
            payloadType = payloadType,
            payloadId = payloadId,
            peerIds = peerIds
        )
}
