package com.cbgm.sparrow.feature.conversationorchestration.message.data.mapper

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.model.UnreadIncomingMessageDto
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage

internal fun MessageEntity.toStoredMessage(): StoredMessage =
    StoredMessage(
        id = id,
        conversationId = conversationId,
        packetId = packetId,
        replyToMessageId = replyToMessageId,
        transportPayload = transportPayload,
        transportMode = transportMode,
        contentStatus = contentStatus.toMessageContentStatus(),
        deliveryStatus = deliveryStatus.toMessageDeliveryStatus(),
        senderContactId = senderContactId,
        readReceiptSent = readReceiptSent,
        isMine = isMine,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds
    )

internal fun StoredMessage.toMessageEntity(): MessageEntity =
    MessageEntity(
        id = id,
        conversationId = conversationId,
        packetId = packetId,
        replyToMessageId = replyToMessageId,
        transportPayload = transportPayload,
        transportMode = transportMode,
        contentStatus = contentStatus.name,
        deliveryStatus = deliveryStatus.name,
        senderContactId = senderContactId,
        readReceiptSent = readReceiptSent,
        isMine = isMine,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds
    )

internal fun UnreadIncomingMessageDto.toUnreadIncomingMessage(): UnreadIncomingMessage =
    UnreadIncomingMessage(
        messageId = messageId,
        conversationId = conversationId,
        contactId = contactId
    )

internal fun String.toMessageDeliveryStatus(): MessageDeliveryStatus =
    MessageDeliveryStatus.entries.firstOrNull { it.name == this } ?: MessageDeliveryStatus.NOT_APPLICABLE

private fun String.toMessageContentStatus(): MessageContentStatus =
    MessageContentStatus.entries.firstOrNull { it.name == this } ?: MessageContentStatus.INVALID_PACKET
