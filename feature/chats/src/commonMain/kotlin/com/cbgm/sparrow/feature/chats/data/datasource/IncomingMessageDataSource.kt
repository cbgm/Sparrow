package com.cbgm.sparrow.feature.chats.data.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.MessageReactionDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity

/** Persistence for direct and group incoming message packets; protocol validation stays with the handlers. */
class IncomingMessageDataSource(
    private val chatDao: ChatDao,
    private val reactionDao: MessageReactionDao
) {
    suspend fun findConversation(conversationId: String): ConversationEntity? =
        chatDao.findConversationById(conversationId)

    suspend fun findMessage(messageId: String): MessageEntity? = chatDao.findMessageById(messageId)

    suspend fun saveMessage(message: MessageEntity) {
        chatDao.upsertMessage(message)
    }

    suspend fun deleteMessages(messages: List<MessageEntity>) {
        chatDao.deleteMessagesAndRefreshConversations(messages)
    }

    suspend fun updateConversationTimestamp(conversationId: String, timestamp: Long) {
        chatDao.updateConversationTimestamp(conversationId, timestamp)
    }

    suspend fun deleteReaction(messageId: String, reactorId: String, emoji: String) {
        reactionDao.delete(messageId, reactorId, emoji)
    }

    suspend fun saveReaction(reaction: MessageReactionEntity) {
        reactionDao.upsert(reaction)
    }
}
