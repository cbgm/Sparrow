package com.cbgm.sparrow.feature.chats.data.group.mapper

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.base.messagepart.data.mapper.toMessagePart
import com.cbgm.sparrow.data.database.entity.GroupPinEntity
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessage
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupPin
import com.cbgm.sparrow.protocol.message.GroupMessageContentCodec

internal fun GroupPinEntity.toDomain(
    groupMessageContentCodec: GroupMessageContentCodec
): GroupPin? {
    val messageId = messageId ?: return null
    val content = groupMessageContentCodec.decode(requireNotNull(messageContent))
    return GroupPin(
        message =
            GroupMessage(
                id = messageId,
                isMine = requireNotNull(isMine),
                timestamp = requireNotNull(messageSentAtEpochMilliseconds),
                security = MessageSecurity.END_TO_END_ENCRYPTED,
                contentStatus = MessageContentStatus.READABLE,
                deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE,
                replyToMessageId = content.replyToMessageId,
                type = ChatMessageType.USER,
                senderContactId = senderContactId,
                parts = content.parts.map { part -> part.toMessagePart() }
            ),
        pinnedAtEpochMilliseconds = requireNotNull(pinnedAtEpochMilliseconds)
    )
}
