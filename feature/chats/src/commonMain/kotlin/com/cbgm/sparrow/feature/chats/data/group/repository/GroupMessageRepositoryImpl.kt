package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMediaAttachment
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupOutgoingMessageProcessor
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository

class GroupMessageRepositoryImpl(
    private val groupInvitationDao: GroupInvitationDao,
    private val outgoingMessageProcessor: GroupOutgoingMessageProcessor
) : GroupMessageRepository {
    override suspend fun send(
        groupId: String,
        text: String,
        media: List<OutgoingMediaAttachment>
    ): Result<Unit> =
        outgoingMessageProcessor.send(
            groupId = groupId,
            text = text,
            media = media,
            invitations = groupInvitationDao.findByGroupId(groupId)
        )

    override suspend fun retry(messageId: String): Result<Unit> =
        outgoingMessageProcessor.retry(messageId)

    override suspend fun markConversationRead(groupId: String): Result<Unit> =
        outgoingMessageProcessor.sendReadReceipts(groupId)
}
