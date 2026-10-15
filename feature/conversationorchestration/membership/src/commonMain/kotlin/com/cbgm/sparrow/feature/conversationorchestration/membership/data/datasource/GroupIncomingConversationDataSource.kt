package com.cbgm.sparrow.feature.conversationorchestration.membership.data.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupMetadataDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.model.GroupSystemMessageDto

/** Persists the group conversation state changed by incoming group lifecycle packets. */
internal class GroupIncomingConversationDataSource(
    private val chatDao: ChatDao,
    private val groupVerificationDao: GroupVerificationDao,
    private val groupMetadataDao: GroupMetadataDao
) {
    suspend fun hasMessageWithTransportMode(conversationId: String, transportMode: String): Boolean =
        chatDao.hasMessageWithTransportMode(conversationId, transportMode)

    suspend fun findMessageTimestampByTransportMode(conversationId: String, transportMode: String): Long? =
        chatDao.findMessageTimestampByTransportMode(conversationId, transportMode)

    suspend fun findConversationParticipants(groupId: String): List<ConversationParticipantEntity> =
        chatDao.findConversationParticipants(groupId)

    suspend fun upsertConversation(conversation: ConversationEntity, title: String) {
        chatDao.upsertConversation(conversation)
        groupMetadataDao.upsertTitleValue(conversation.id, title)
    }

    suspend fun upsertMessage(message: GroupSystemMessageDto) {
        chatDao.upsertMessageWithText(message.message, message.text)
    }

    suspend fun replaceConversationParticipantsWithMessages(
        conversationId: String,
        participants: List<ConversationParticipantEntity>,
        messages: List<GroupSystemMessageDto>
    ) {
        chatDao.replaceConversationParticipants(conversationId, participants)
        messages.forEach { message -> chatDao.upsertMessageWithText(message.message, message.text) }
    }

    suspend fun applyLocalGroupRemoval(message: GroupSystemMessageDto) {
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
