package com.cbgm.sparrow.feature.chats.data.direct.datasource

import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.MessageReactionDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DirectConversationDataSource(
    private val chatDao: ChatDao,
    private val reactionDao: MessageReactionDao
) {
    fun observeConversationById(conversationId: String): Flow<ConversationEntity?> =
        chatDao.observeConversationById(conversationId)

    fun observeRecentMessages(conversationId: String, limit: Int): Flow<List<MessageEntity>> =
        chatDao.observeRecentMessages(conversationId, limit)

    fun observeMessagesFromCursor(
        conversationId: String,
        fromTimestamp: Long,
        fromMessageId: String
    ): Flow<List<MessageEntity>> =
        chatDao.observeMessagesFromCursor(conversationId, fromTimestamp, fromMessageId)

    fun observeTextPartsByMessageIds(messageIds: List<String>): Flow<Map<String, List<MessagePartDto>>> =
        combine(
            chatDao.observeTextPartEntitiesByMessageIds(messageIds),
            chatDao.observeTextEntitiesByMessageIds(messageIds)
        ) { parts, texts ->
            val textByPartId = texts.associateBy { text -> text.partId }
            parts.groupBy { part -> part.messageId }
                .mapValues { (_, messageParts) ->
                    messageParts.sortedBy { part -> part.position }
                        .mapNotNull { part ->
                            textByPartId[part.id]?.let { text -> TextDto(id = part.id, text = text.text) }
                        }
                }
        }

    fun observeRecentReactions(conversationId: String, messageLimit: Int): Flow<List<MessageReactionEntity>> =
        reactionDao.observeRecentByConversationId(conversationId, messageLimit)

    fun observeReactionsFromCursor(
        conversationId: String,
        fromTimestamp: Long,
        fromMessageId: String
    ): Flow<List<MessageReactionEntity>> =
        reactionDao.observeFromMessageCursor(conversationId, fromTimestamp, fromMessageId)

    suspend fun findConversationByContactId(contactId: String): ConversationEntity? =
        chatDao.findConversationByContactId(contactId)

    suspend fun findConversationById(conversationId: String): ConversationEntity? =
        chatDao.findConversationById(conversationId)
}
