package com.cbgm.sparrow.feature.chats.data.direct.datasource

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationType

class DirectConversationDataSource(
    private val chatDao: ChatDao
) {
    suspend fun getOrCreate(contactId: String): ConversationEntity {
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }

        chatDao.findConversationByContactId(contactId)?.let { conversation ->
            return conversation
        }

        val now = SystemClock.nowEpochMilliseconds()
        val conversation =
            ConversationEntity(
                id = IdGenerator.generate(prefix = "conversation"),
                contactId = contactId,
                type = ConversationType.DIRECT.name,
                title = null,
                createdAtEpochMilliseconds = now,
                updatedAtEpochMilliseconds = now
            )

        chatDao.upsertConversation(conversation)

        return chatDao.findConversationByContactId(contactId)
            ?: error("Conversation could not be created")
    }
}
