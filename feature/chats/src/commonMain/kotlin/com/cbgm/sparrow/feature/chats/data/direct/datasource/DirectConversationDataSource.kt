package com.cbgm.sparrow.feature.chats.data.direct.datasource

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.MessageReactionDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationType
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.data.database.model.UnreadIncomingMessageDto
import kotlinx.coroutines.flow.Flow

class DirectConversationDataSource(
    private val chatDao: ChatDao,
    private val reactionDao: MessageReactionDao
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

    suspend fun deleteConversation(conversationId: String) =
        chatDao.deleteConversation(conversationId)

    suspend fun findConversationById(conversationId: String): ConversationEntity? =
        chatDao.findConversationById(conversationId)

    suspend fun findMessageById(messageId: String): MessageEntity? =
        chatDao.findMessageById(messageId)

    suspend fun upsertMessage(message: MessageEntity) =
        chatDao.upsertMessage(message)

    suspend fun upsertIncomingChatMessage(
        conversation: ConversationEntity,
        message: MessageEntity,
        timestamp: Long
    ) = chatDao.upsertIncomingChatMessage(
        conversation = conversation,
        message = message,
        timestamp = timestamp
    )

    suspend fun deleteMessage(messageId: String) =
        chatDao.deleteMessagesByIds(listOf(messageId))

    suspend fun deleteMessages(messages: List<MessageEntity>) =
        chatDao.deleteMessagesAndRefreshConversations(messages)

    suspend fun findMessagesAwaitingReadReceipt(conversationId: String): List<UnreadIncomingMessageDto> =
        chatDao.findMessagesAwaitingReadReceipt(conversationId)

    suspend fun markReadReceiptSent(messageId: String): Boolean =
        chatDao.markReadReceiptSent(messageId) == 1

    fun observeDirectMessagesByDeliveryStatus(deliveryStatus: String): Flow<List<MessageEntity>> =
        chatDao.observeDirectMessagesByDeliveryStatus(deliveryStatus)

    suspend fun findMessagesWaitingForAuthorization(contactId: String): List<MessageEntity> =
        chatDao.findDirectMessagesByContactAndDeliveryStatus(
            contactId = contactId,
            deliveryStatus = "WAITING_FOR_AUTHORIZATION"
        )
}
