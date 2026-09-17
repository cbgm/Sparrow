package com.cbgm.sparrow.feature.chats.data.group.delivery

import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.invite.data.outbox.InvitationOutboxDeliveryHandler

class GroupOutboxDeliveryHandler internal constructor(
    private val deliveryCoordinator: GroupMessageDeliveryCoordinator,
    private val invitationDeliveryHandler: InvitationOutboxDeliveryHandler
) {
    suspend fun canHandle(packetId: String): Boolean =
        invitationDeliveryHandler.canHandle(packetId) || deliveryCoordinator.handlesPacket(packetId)

    suspend fun applyEvent(
        packetId: String,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ) {
        if (invitationDeliveryHandler.canHandle(packetId)) {
            if (event == MessageDeliveryEvent.SEND_FAILED) {
                invitationDeliveryHandler.onFailed(packetId)
            }
            return
        }
        deliveryCoordinator.applyPacketEvent(packetId, event, errorMessage)
    }
}
