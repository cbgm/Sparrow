package com.cbgm.sparrow.feature.chats.data.group.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.Contact
import com.cbgm.sparrow.core.messagepart.domain.model.File
import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.core.messagepart.domain.model.Location
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.Text
import com.cbgm.sparrow.core.messagepart.domain.model.Video
import com.cbgm.sparrow.core.messagepart.domain.model.Voice
import com.cbgm.sparrow.data.database.entity.GroupPinEntity
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessage
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupPin
import com.cbgm.sparrow.protocol.attachment.MessageAttachment
import com.cbgm.sparrow.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.protocol.message.GroupMessageContent
import com.cbgm.sparrow.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket

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
                parts =
                    buildList {
                        content.text
                            .takeIf(String::isNotBlank)
                            ?.let { text -> add(Text(id = messageId, text = text)) }
                        addAll(content.attachments.map(MessageAttachment::toMessagePart))
                    }
            ),
        pinnedAtEpochMilliseconds = requireNotNull(pinnedAtEpochMilliseconds)
    )
}

internal fun createGroupPinEntity(
    groupId: String,
    messageId: String,
    messageSentAtEpochMilliseconds: Long,
    messageSenderSigningPublicKey: ByteArray,
    senderContactId: String?,
    isMine: Boolean,
    content: GroupMessageContent,
    pinnedAtEpochMilliseconds: Long,
    changedAtEpochMilliseconds: Long,
    groupMessageContentCodec: GroupMessageContentCodec
): GroupPinEntity =
    GroupPinEntity(
        groupId = groupId,
        messageId = messageId,
        messageSentAtEpochMilliseconds = messageSentAtEpochMilliseconds,
        messageSenderSigningPublicKey = messageSenderSigningPublicKey.copyOf(),
        senderContactId = senderContactId,
        isMine = isMine,
        messageContent = groupMessageContentCodec.encode(content),
        pinnedAtEpochMilliseconds = pinnedAtEpochMilliseconds,
        changedAtEpochMilliseconds = changedAtEpochMilliseconds
    )

internal fun GroupPinUpdatedPacket.toEntity(
    senderContactId: String?,
    isMine: Boolean,
    groupMessageContentCodec: GroupMessageContentCodec
): GroupPinEntity {
    val pinnedMessageId = messageId
    return if (pinnedMessageId == null) {
        unpinnedGroupPinEntity(
            groupId = groupId,
            changedAtEpochMilliseconds = changedAtEpochMilliseconds
        )
    } else {
        createGroupPinEntity(
            groupId = groupId,
            messageId = pinnedMessageId,
            messageSentAtEpochMilliseconds = messageSentAtEpochMilliseconds,
            messageSenderSigningPublicKey = messageSenderSigningPublicKey,
            senderContactId = senderContactId,
            isMine = isMine,
            content = requireNotNull(messageContent),
            pinnedAtEpochMilliseconds = changedAtEpochMilliseconds,
            changedAtEpochMilliseconds = changedAtEpochMilliseconds,
            groupMessageContentCodec = groupMessageContentCodec
        )
    }
}

internal fun unpinnedGroupPinEntity(
    groupId: String,
    changedAtEpochMilliseconds: Long
): GroupPinEntity =
    GroupPinEntity(
        groupId = groupId,
        messageId = null,
        messageSentAtEpochMilliseconds = null,
        messageSenderSigningPublicKey = null,
        senderContactId = null,
        isMine = null,
        messageContent = null,
        pinnedAtEpochMilliseconds = null,
        changedAtEpochMilliseconds = changedAtEpochMilliseconds
    )

private fun MessageAttachment.toMessagePart(): MessagePart =
    when (type) {
        MessageAttachmentType.IMAGE ->
            Image(
                id = attachmentId,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                width = width,
                height = height
            )

        MessageAttachmentType.VIDEO ->
            Video(
                id = attachmentId,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds
            )

        MessageAttachmentType.FILE ->
            File(
                id = attachmentId,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName ?: attachmentId
            )

        MessageAttachmentType.LOCATION -> Location(id = attachmentId)
        MessageAttachmentType.CONTACT -> Contact(id = attachmentId)
        MessageAttachmentType.VOICE ->
            Voice(
                id = attachmentId,
                mimeType = mimeType,
                byteSize = byteSize,
                durationMilliseconds = requireNotNull(durationMilliseconds)
            )
    }
