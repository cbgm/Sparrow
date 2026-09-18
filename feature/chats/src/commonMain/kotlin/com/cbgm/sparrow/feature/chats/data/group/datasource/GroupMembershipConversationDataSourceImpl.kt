package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipConversationDataSource

internal class GroupMembershipConversationDataSourceImpl(
    private val chatDao: ChatDao
) : GroupMembershipConversationDataSource {
    override suspend fun findConversationById(conversationId: String): ConversationEntity? =
        chatDao.findConversationById(conversationId)

    override suspend fun findConversationParticipants(conversationId: String): List<ConversationParticipantEntity> =
        chatDao.findConversationParticipants(conversationId)

    override suspend fun upsertConversationParticipant(participant: ConversationParticipantEntity) {
        chatDao.upsertConversationParticipant(participant)
    }

    override suspend fun updateConversationParticipantRole(
        conversationId: String,
        contactId: String,
        role: String
    ): Int = chatDao.updateConversationParticipantRole(conversationId, contactId, role)

    override suspend fun deleteConversationParticipant(
        conversationId: String,
        contactId: String
    ) {
        chatDao.deleteConversationParticipant(conversationId, contactId)
    }

    override suspend fun upsertMessage(message: MessageEntity) {
        chatDao.upsertMessage(message)
    }

    override suspend fun updateConversationTimestamp(
        conversationId: String,
        timestamp: Long
    ) {
        chatDao.updateConversationTimestamp(conversationId, timestamp)
    }
}
