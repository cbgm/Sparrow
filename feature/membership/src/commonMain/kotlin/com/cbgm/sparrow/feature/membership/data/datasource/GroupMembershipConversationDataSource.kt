package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity

interface GroupMembershipConversationDataSource {
    suspend fun findConversationById(conversationId: String): ConversationEntity?

    suspend fun findConversationParticipants(conversationId: String): List<ConversationParticipantEntity>

    suspend fun upsertConversationParticipant(participant: ConversationParticipantEntity)

    suspend fun updateConversationParticipantRole(
        conversationId: String,
        contactId: String,
        role: String
    ): Int

    suspend fun deleteConversationParticipant(
        conversationId: String,
        contactId: String
    )

    suspend fun upsertMessage(message: MessageEntity)

    suspend fun updateConversationTimestamp(
        conversationId: String,
        timestamp: Long
    )
}
