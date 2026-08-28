package com.cbgm.sparrow.feature.chats.data.direct.delivery

import com.cbgm.sparrow.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.sparrow.feature.chats.data.direct.mapper.toMessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectMessageDeliveryStateMachine

class DirectMessageDeliveryCoordinator(
    private val messageDeliveryStatusDao: MessageDeliveryStatusDao
) {
    suspend fun handlesPacket(packetId: String): Boolean =
        messageDeliveryStatusDao.findOutgoingDeliveryStatusByPacketId(packetId) != null

    suspend fun handlesReceipt(
        messageId: String,
        contactId: String
    ): Boolean =
        messageDeliveryStatusDao.findOutgoingDeliveryStatus(messageId, contactId) != null

    suspend fun storePreparedTransport(
        packetId: String,
        encodedTransportPayload: String,
        transportMode: String
    ) {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(encodedTransportPayload.isNotBlank()) { "Transport payload must not be blank" }
        require(transportMode.isNotBlank()) { "Transport mode must not be blank" }
        messageDeliveryStatusDao.updatePreparedTransport(packetId, encodedTransportPayload, transportMode)
    }

    suspend fun applyPacketEvent(
        packetId: String,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ) {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        val current = messageDeliveryStatusDao.findOutgoingDeliveryStatusByPacketId(packetId)?.toMessageDeliveryStatus() ?: return
        val next = DirectMessageDeliveryStateMachine.transition(current, event)
        if (next != current) {
            messageDeliveryStatusDao.updateDeliveryStatus(packetId, next.name)
        }
    }

    suspend fun applyReceiptEvent(
        messageId: String,
        contactId: String,
        event: MessageDeliveryEvent
    ) {
        requireReceiptEvent(messageId, contactId, event)
        val current = messageDeliveryStatusDao.findOutgoingDeliveryStatus(messageId, contactId)?.toMessageDeliveryStatus() ?: return
        val next = DirectMessageDeliveryStateMachine.transition(current, event)
        if (next != current) {
            messageDeliveryStatusDao.updateDeliveryStatusByMessageId(messageId, next.name)
        }
    }

    suspend fun applyRetryEvent(messageId: String) {
        require(messageId.isNotBlank()) { "Message ID must not be blank" }
        val current = messageDeliveryStatusDao.findOutgoingDeliveryStatusByMessageId(messageId)?.toMessageDeliveryStatus() ?: return
        val next = DirectMessageDeliveryStateMachine.transition(current, MessageDeliveryEvent.RETRY_REQUESTED)
        if (next != current) {
            messageDeliveryStatusDao.updateDeliveryStatusByMessageId(messageId, next.name)
        }
    }

    private fun requireReceiptEvent(
        messageId: String,
        contactId: String,
        event: MessageDeliveryEvent
    ) {
        require(messageId.isNotBlank()) { "Message ID must not be blank" }
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        require(event == MessageDeliveryEvent.DELIVERY_CONFIRMED || event == MessageDeliveryEvent.READ_CONFIRMED) {
            "Only receipt events can be applied by message ID"
        }
    }
}
