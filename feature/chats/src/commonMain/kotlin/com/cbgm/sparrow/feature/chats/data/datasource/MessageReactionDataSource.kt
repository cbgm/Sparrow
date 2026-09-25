package com.cbgm.sparrow.feature.chats.data.datasource

import com.cbgm.sparrow.data.database.dao.MessageReactionDao
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity

class MessageReactionDataSource(
    private val dao: MessageReactionDao
) {
    suspend fun find(
        messageId: String,
        reactorId: String,
        emoji: String
    ): MessageReactionEntity? = dao.find(messageId, reactorId, emoji)

    suspend fun upsert(entity: MessageReactionEntity) {
        dao.upsert(entity)
    }

    suspend fun delete(
        messageId: String,
        reactorId: String,
        emoji: String
    ) {
        dao.delete(messageId, reactorId, emoji)
    }
}
