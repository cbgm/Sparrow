package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupSystemMessage

/** Persists Chats-owned state modified by incoming group lifecycle packets. */
class GroupIncomingConversationDataSource(
    private val chatDao: ChatDao,
    private val groupVerificationDao: GroupVerificationDao
) {
    suspend fun hasMessageWithTransportMode(conversationId: String, transportMode: String): Boolean =
        chatDao.hasMessageWithTransportMode(conversationId, transportMode)

    suspend fun findMessageTimestampByTransportMode(conversationId: String, transportMode: String): Long? =
        chatDao.findMessageTimestampByTransportMode(conversationId, transportMode)

    suspend fun findConversationParticipants(groupId: String): List<ConversationParticipantEntity> =
        chatDao.findConversationParticipants(groupId)

    suspend fun upsertConversation(conversation: ConversationEntity) {
        chatDao.upsertConversation(conversation)
    }

    internal suspend fun upsertMessage(message: GroupSystemMessage) {
        chatDao.upsertMessageWithText(message.message, message.text)
    }

    internal suspend fun replaceConversationParticipantsWithMessages(
        conversationId: String,
        participants: List<ConversationParticipantEntity>,
        messages: List<GroupSystemMessage>
    ) {
        chatDao.replaceConversationParticipants(conversationId, participants)
        messages.forEach { message -> chatDao.upsertMessageWithText(message.message, message.text) }
    }

    internal suspend fun applyLocalGroupRemoval(message: GroupSystemMessage) {
        chatDao.applyLocalGroupRemoval(message.message, message.text)
    }

    suspend fun deleteConversationParticipants(groupId: String) {
        chatDao.deleteConversationParticipants(groupId)
    }

    suspend fun deleteVerificationRows(groupId: String) {
        groupVerificationDao.deleteByGroupId(groupId)
    }

    suspend fun updateConversationTimestamp(conversationId: String, timestamp: Long) {
        chatDao.updateConversationTimestamp(conversationId, timestamp)
    }
}
