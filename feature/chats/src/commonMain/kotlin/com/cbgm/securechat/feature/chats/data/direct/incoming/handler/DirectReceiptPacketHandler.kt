package com.cbgm.securechat.feature.chats.data.direct.incoming.handler

import com.cbgm.securechat.feature.chats.data.direct.delivery.DirectMessageDeliveryCoordinator
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryEvent

class DirectReceiptPacketHandler(
    private val deliveryCoordinator: DirectMessageDeliveryCoordinator
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
