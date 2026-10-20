package com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource

import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationType

internal class ConversationDataSource(
    private val chatDao: ChatDao
) {
    suspend fun getOrCreateDirect(contactId: String): ConversationEntity {
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }

        chatDao.findConversationByContactId(contactId)?.let { conversation ->
            return conversation
        }

        val now = SystemClock.nowEpochMilliseconds()
        chatDao.upsertConversation(
            ConversationEntity(
                id = IdGenerator.generate(prefix = "conversation"),
                contactId = contactId,
                type = ConversationType.DIRECT.name,
                createdAtEpochMilliseconds = now,
                updatedAtEpochMilliseconds = now
            )
        )
        return chatDao.findConversationByContactId(contactId)
            ?: error("Conversation could not be created")
    }

    suspend fun findById(conversationId: String): ConversationEntity? = chatDao.findConversationById(conversationId)

    suspend fun findByContactId(contactId: String): ConversationEntity? = chatDao.findConversationByContactId(contactId)

    suspend fun delete(conversationId: String) {
        chatDao.deleteConversation(conversationId)
    }

    suspend fun findParticipantIds(groupId: String): List<String> =
        chatDao.findConversationParticipants(groupId).map { participant -> participant.contactId }

    suspend fun incrementUnseenLocalMessageCount(conversationId: String): Int =
        chatDao.incrementUnseenLocalMessageCount(conversationId)

    suspend fun clearUnseenLocalMessageCount(conversationId: String): Int =
        chatDao.clearUnseenLocalMessageCount(conversationId)
}
