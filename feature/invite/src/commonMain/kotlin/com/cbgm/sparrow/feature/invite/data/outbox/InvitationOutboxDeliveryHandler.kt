package com.cbgm.sparrow.feature.invite.data.outbox

import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.MarkInvitationTransportFailedUseCase

class InvitationOutboxDeliveryHandler internal constructor(
    private val markInvitationTransportFailed: MarkInvitationTransportFailedUseCase
) {
    fun canHandle(packetId: String): Boolean =
        packetId.startsWith(GROUP_INVITE_PACKET_ID_PREFIX) &&
            !packetId.startsWith(GROUP_INVITE_RECEIVED_PACKET_ID_PREFIX)

    suspend fun onFailed(packetId: String) {
        if (!canHandle(packetId)) return
        val invitationId = packetId.removePrefix(GROUP_INVITE_PACKET_ID_PREFIX)
        if (invitationId.isBlank()) return
        markInvitationTransportFailed(
            payloadType = InvitationPayloadType.GROUP,
            invitationId = invitationId
        ).getOrThrow()
    }

    private companion object {
        const val GROUP_INVITE_PACKET_ID_PREFIX = "group-invite-"
        const val GROUP_INVITE_RECEIVED_PACKET_ID_PREFIX = "group-invite-received-"
    }
}
