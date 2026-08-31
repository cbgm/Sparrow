package com.cbgm.sparrow.feature.chats.data.direct.mapper

import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.model.ConversationWithMessagesDto
import com.cbgm.sparrow.feature.chats.data.mapper.toMessagePart
import com.cbgm.sparrow.feature.chats.data.model.MessagePartDto
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectMessage

internal fun ConversationWithMessagesDto.toDirectConversation(
    partsByMessageId: Map<String, List<MessagePartDto>> = emptyMap()
): DirectConversation =
    DirectConversation(
        id = conversation.id,
        contactId = requireNotNull(conversation.contactId) { "Direct conversation has no contact" },
        messages =
            messages
                .sortedBy(MessageEntity::createdAtEpochMilliseconds)
                .map { message ->
                    message.toDirectMessage(
                        contactId = requireNotNull(conversation.contactId),
                        attachmentParts = partsByMessageId[message.id].orEmpty()
                    )
                },
        unreadCount =
            messages.count { message ->
                !message.isMine &&
                    !message.readReceiptSent &&
                    message.contentStatus == MessageContentStatus.READABLE.name
            }
    )

private fun MessageEntity.toDirectMessage(
    contactId: String,
    attachmentParts: List<MessagePartDto>
): DirectMessage =
    DirectMessage(
        id = id,
        contactId = contactId,
        isMine = isMine,
        timestamp = createdAtEpochMilliseconds,
        security = transportMode.toMessageSecurity(),
        contentStatus = contentStatus.toMessageContentStatus(),
        deliveryStatus =
            if (isMine) {
                deliveryStatus.toMessageDeliveryStatus()
            } else {
                MessageDeliveryStatus.NOT_APPLICABLE
            },
        replyToMessageId = replyToMessageId,
        parts =
            buildList {
                text
                    .takeIf(String::isNotBlank)
                    ?.let { value -> add(MessagePartDto.TextDto(text = value)) }
                addAll(attachmentParts)
            }.map { part -> part.toMessagePart() }
    )

private fun String.toMessageSecurity(): MessageSecurity =
    if (this == TransportEncryptionMode.SEALED_BOX.name) {
        MessageSecurity.END_TO_END_ENCRYPTED
    } else {
        MessageSecurity.INSECURE
    }

private fun String.toMessageContentStatus(): MessageContentStatus =
    MessageContentStatus.entries.firstOrNull { it.name == this }
        ?: MessageContentStatus.INVALID_PACKET

internal fun String.toMessageDeliveryStatus(): MessageDeliveryStatus =
    MessageDeliveryStatus.entries.firstOrNull { it.name == this }
        ?: MessageDeliveryStatus.NOT_APPLICABLE
