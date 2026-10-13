package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.mapper

import com.cbgm.sparrow.core.messagepart.data.mapper.toDto
import com.cbgm.sparrow.core.messagepart.data.mapper.toMessagePart
import com.cbgm.sparrow.data.database.entity.GroupPinEntity
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.protocol.message.GroupMessageContent
import com.cbgm.sparrow.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket

internal fun GroupPinEntity.toGroupPinState(groupMessageContentCodec: GroupMessageContentCodec): GroupPinState {
    val content = messageContent?.let(groupMessageContentCodec::decode)
    return GroupPinState(
        groupId = groupId,
        messageId = messageId,
        messageSentAtEpochMilliseconds = messageSentAtEpochMilliseconds,
        messageSenderSigningPublicKey = messageSenderSigningPublicKey?.copyOf(),
        senderContactId = senderContactId,
        isMine = isMine,
        parts = content?.parts?.map { part -> part.toMessagePart() },
        replyToMessageId = content?.replyToMessageId,
        pinnedAtEpochMilliseconds = pinnedAtEpochMilliseconds,
        changedAtEpochMilliseconds = changedAtEpochMilliseconds
    )
}

internal fun GroupPinState.toGroupPinEntity(groupMessageContentCodec: GroupMessageContentCodec): GroupPinEntity =
    GroupPinEntity(
        groupId = groupId,
        messageId = messageId,
        messageSentAtEpochMilliseconds = messageSentAtEpochMilliseconds,
        messageSenderSigningPublicKey = messageSenderSigningPublicKey?.copyOf(),
        senderContactId = senderContactId,
        isMine = isMine,
        messageContent = toGroupMessageContent()?.let(groupMessageContentCodec::encode),
        pinnedAtEpochMilliseconds = pinnedAtEpochMilliseconds,
        changedAtEpochMilliseconds = changedAtEpochMilliseconds
    )

/** The pinned snapshot is serialized exactly like a group message. */
internal fun GroupPinState.toGroupMessageContent(): GroupMessageContent? =
    parts?.let { pinnedParts ->
        GroupMessageContent(
            parts = pinnedParts.map { part -> part.toDto() },
            replyToMessageId = replyToMessageId
        )
    }

internal fun GroupPinUpdatedPacket.toGroupPinState(senderContactId: String?, isMine: Boolean): GroupPinState {
    val pinnedMessageId = messageId ?: return GroupPinState.unpinned(groupId, changedAtEpochMilliseconds)
    val content = requireNotNull(messageContent)
    return GroupPinState(
        groupId = groupId,
        messageId = pinnedMessageId,
        messageSentAtEpochMilliseconds = messageSentAtEpochMilliseconds,
        messageSenderSigningPublicKey = messageSenderSigningPublicKey.copyOf(),
        senderContactId = senderContactId,
        isMine = isMine,
        parts = content.parts.map { part -> part.toMessagePart() },
        replyToMessageId = content.replyToMessageId,
        pinnedAtEpochMilliseconds = changedAtEpochMilliseconds,
        changedAtEpochMilliseconds = changedAtEpochMilliseconds
    )
}
