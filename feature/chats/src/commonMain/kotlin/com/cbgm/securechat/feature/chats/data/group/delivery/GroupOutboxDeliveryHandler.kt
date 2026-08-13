package com.cbgm.securechat.feature.chats.data.group.delivery

import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryEvent

class GroupOutboxDeliveryHandler(
    private val deliveryCoordinator: GroupMessageDeliveryCoordinator
) {
    suspend fun canHandle(packetId: String): Boolean =
        deliveryCoordinator.handlesPacket(packetId)

    suspend fun applyEvent(
        packetId: String,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ) {
        deliveryCoordinator.applyPacketEvent(packetId, event, errorMessage)
    }
}
