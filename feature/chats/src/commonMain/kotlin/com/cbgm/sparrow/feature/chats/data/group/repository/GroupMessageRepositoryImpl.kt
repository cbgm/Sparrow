package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupOutgoingMessageProcessor
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository
import com.cbgm.sparrow.feature.membership.domain.model.GroupMessageMembershipAccess

class GroupMessageRepositoryImpl(
    private val outgoingMessageProcessor: GroupOutgoingMessageProcessor
) : GroupMessageRepository {
    override suspend fun send(
        groupId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment>,
        replyToMessageId: String?,
        access: GroupMessageMembershipAccess
    ): Result<Unit> =
        outgoingMessageProcessor.send(
            groupId = groupId,
            text = text,
            attachments = attachments,
            replyToMessageId = replyToMessageId,
            access = access
        )

    override suspend fun toggleReaction(
        groupId: String,
        messageId: String,
        emoji: String,
        access: GroupMessageMembershipAccess
    ): Result<Unit> =
        safeSuspendCall {
            outgoingMessageProcessor.toggleReaction(
                groupId = groupId,
                messageId = messageId,
                emoji = emoji,
                access = access
            )
        }

    override suspend fun deleteMessage(groupId: String, messageId: String, access: GroupMessageMembershipAccess): Result<Unit> =
        safeSuspendCall {
            outgoingMessageProcessor.deleteMessage(
                groupId = groupId,
                messageId = messageId,
                access = access
            )
        }

    override suspend fun editMessage(
        groupId: String,
        messageId: String,
        text: String,
        access: GroupMessageMembershipAccess
    ): Result<Unit> = safeSuspendCall {
        outgoingMessageProcessor.editMessage(
            groupId = groupId,
            messageId = messageId,
            text = text,
            access = access
        )
    }

    override suspend fun retry(messageId: String): Result<Unit> = safeSuspendCall {
        outgoingMessageProcessor.retry(messageId)
    }

    override suspend fun markConversationRead(groupId: String): Result<Unit> = safeSuspendCall {
        outgoingMessageProcessor.sendReadReceipts(groupId)
    }
}
