package com.cbgm.securechat.feature.chats.data.group.incoming.handler

import com.cbgm.securechat.feature.chats.data.group.delivery.GroupMessageDeliveryCoordinator
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryEvent

class GroupReceiptPacketHandler(
    private val deliveryCoordinator: GroupMessageDeliveryCoordinator
) {
    suspend fun canHandle(
        messageId: String,
        contactId: String
    ): Boolean = deliveryCoordinator.handlesReceipt(messageId, contactId)

    suspend fun handle(
        messageId: String,
        contactId: String,
        event: MessageDeliveryEvent
    ) {
        deliveryCoordinator.applyReceiptEvent(
            messageId = messageId,
            contactId = contactId,
            event = event
        )
    }
}
