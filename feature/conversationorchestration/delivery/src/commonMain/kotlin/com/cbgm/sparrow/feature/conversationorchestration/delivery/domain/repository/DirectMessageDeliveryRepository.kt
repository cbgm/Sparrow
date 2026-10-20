package com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.repository

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus

interface DirectMessageDeliveryRepository {
    suspend fun findStatusByPacketId(packetId: String): Result<MessageDeliveryStatus?>

    suspend fun findStatusByRecipient(messageId: String, contactId: String): Result<MessageDeliveryStatus?>

    suspend fun findStatusByMessageId(messageId: String): Result<MessageDeliveryStatus?>

    suspend fun updateStatusByPacketId(packetId: String, status: MessageDeliveryStatus): Result<Unit>

    suspend fun updateStatusByMessageId(messageId: String, status: MessageDeliveryStatus): Result<Unit>

    suspend fun storePreparedTransport(
        packetId: String,
        encodedTransportPayload: String,
        transportMode: String
    ): Result<Unit>
}
