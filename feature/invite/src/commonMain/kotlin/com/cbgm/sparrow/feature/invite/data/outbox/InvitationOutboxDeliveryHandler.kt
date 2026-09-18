package com.cbgm.sparrow.feature.invite.data.outbox

import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationTransportFailedUseCase

class InvitationOutboxDeliveryHandler internal constructor(
    private val markInvitationTransportFailed: MarkInvitationTransportFailedUseCase
) {
    suspend fun onFailed(
        payloadType: InvitationPayloadType,
        invitationId: String
    ) {
        if (invitationId.isBlank()) return

        markInvitationTransportFailed(
            payloadType = payloadType,
            invitationId = invitationId
        ).getOrThrow()
    }
}
