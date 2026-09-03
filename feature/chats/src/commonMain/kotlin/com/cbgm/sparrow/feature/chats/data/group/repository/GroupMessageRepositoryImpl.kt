package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupOutgoingMessageProcessor
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository

class GroupMessageRepositoryImpl(
    private val groupInvitationDao: GroupInvitationDao,
    private val outgoingMessageProcessor: GroupOutgoingMessageProcessor
) : GroupMessageRepository {
    override suspend fun send(
        groupId: String,
        text: String,
        attachments: List<OutgoingMessageAttachment>,
        replyToMessageId: String?
    ): Result<Unit> =
        outgoingMessageProcessor.send(
            groupId = groupId,
            text = text,
            attachments = attachments,
            replyToMessageId = replyToMessageId,
            invitations = groupInvitationDao.findByGroupId(groupId)
        )

    override suspend fun toggleReaction(groupId: String, messageId: String, emoji: String): Result<Unit> =
        outgoingMessageProcessor.toggleReaction(
            groupId = groupId,
            messageId = messageId,
            emoji = emoji,
            invitations = groupInvitationDao.findByGroupId(groupId)
        )

    override suspend fun deleteMessage(groupId: String, messageId: String): Result<Unit> =
        outgoingMessageProcessor.deleteMessage(
            groupId = groupId,
            messageId = messageId,
            invitations = groupInvitationDao.findByGroupId(groupId)
        )

    override suspend fun retry(messageId: String): Result<Unit> =
        outgoingMessageProcessor.retry(messageId)

    override suspend fun markConversationRead(groupId: String): Result<Unit> =
        outgoingMessageProcessor.sendReadReceipts(groupId)
}
