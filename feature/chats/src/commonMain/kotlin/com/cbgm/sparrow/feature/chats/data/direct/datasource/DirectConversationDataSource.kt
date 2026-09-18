package com.cbgm.sparrow.feature.chats.data.direct.datasource

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationType
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.model.UnreadIncomingMessageDto

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

    suspend fun findMessagesWaitingForAuthorization(contactId: String): List<MessageEntity> =
        chatDao.findDirectMessagesByContactAndDeliveryStatus(
            contactId = contactId,
            deliveryStatus = "WAITING_FOR_AUTHORIZATION"
        )
}
