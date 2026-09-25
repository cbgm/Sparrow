package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity

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

    suspend fun upsertMessage(message: MessageEntity) {
        chatDao.upsertMessage(message)
    }

    suspend fun replaceConversationParticipantsWithMessages(
        conversationId: String,
        participants: List<ConversationParticipantEntity>,
        messages: List<MessageEntity>
    ) {
        // Keep ChatDao's existing transaction boundary for participants and their history events.
        chatDao.replaceConversationParticipantsWithMessages(conversationId, participants, messages)
    }

    suspend fun upsertConversationParticipant(participant: ConversationParticipantEntity) {
        chatDao.upsertConversationParticipant(participant)
    }

    suspend fun applyLocalGroupRemoval(message: MessageEntity) {
        chatDao.applyLocalGroupRemoval(message)
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
