package com.cbgm.securechat.feature.chats.data.direct.mapper

import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.model.ConversationWithMessages
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageSecurity
import com.cbgm.securechat.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.securechat.feature.chats.domain.model.direct.DirectMessage

internal fun ConversationWithMessages.toDirectConversation(): DirectConversation =
    DirectConversation(
        id = conversation.id,
        contactId = requireNotNull(conversation.contactId) { "Direct conversation has no contact" },
        messages =
            messages
                .sortedBy(MessageEntity::createdAtEpochMilliseconds)
                .map { message -> message.toDirectMessage(requireNotNull(conversation.contactId)) },
        unreadCount =
            messages.count { message ->
                !message.isMine &&
                    !message.readReceiptSent &&
                    message.contentStatus == MessageContentStatus.READABLE.name
            }
    )

private fun MessageEntity.toDirectMessage(contactId: String): DirectMessage =
    DirectMessage(
        id = id,
        contactId = contactId,
        text = text,
        isMine = isMine,
        timestamp = createdAtEpochMilliseconds,
        security = transportMode.toMessageSecurity(),
        contentStatus = contentStatus.toMessageContentStatus(),
        deliveryStatus =
            if (isMine) {
                deliveryStatus.toMessageDeliveryStatus()
            } else {
                MessageDeliveryStatus.NOT_APPLICABLE
            }
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

internal fun String.toDirectDeliveryStatus(): MessageDeliveryStatus =
    MessageDeliveryStatus.entries.firstOrNull { it.name == this }
        ?: MessageDeliveryStatus.NOT_APPLICABLE

private fun String.toMessageDeliveryStatus(): MessageDeliveryStatus = toDirectDeliveryStatus()
