package com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.model.UnreadIncomingMessageDto
import kotlinx.coroutines.flow.Flow

internal class DirectMessageDataSource(
    private val chatDao: ChatDao
) {
    suspend fun findMessage(messageId: String): MessageEntity? = chatDao.findMessageById(messageId)

    suspend fun findMessageText(messageId: String): String? = chatDao.findMessageText(messageId)

    suspend fun upsertMessage(message: MessageEntity) {
        chatDao.upsertMessage(message)
    }

    suspend fun upsertMessageWithText(message: MessageEntity, text: String) {
        chatDao.upsertMessageWithText(message, text)
    }

    suspend fun replaceMessageText(messageId: String, text: String) {
        chatDao.replaceMessageText(messageId, text)
    }

    suspend fun deleteMessages(messages: List<MessageEntity>) {
        chatDao.deleteMessagesAndRefreshConversations(messages)
    }

    suspend fun findMessagesWaitingForAuthorization(contactId: String): List<MessageEntity> =
        chatDao.findDirectMessagesByContactAndDeliveryStatus(
            contactId = contactId,
            deliveryStatus = MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION.name
        )

    fun observeMessagesWaitingForAuthorization(): Flow<List<MessageEntity>> =
        chatDao.observeDirectMessagesByDeliveryStatus(MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION.name)

    suspend fun findMessagesAwaitingOutbox(createdBefore: Long): List<MessageEntity> =
        chatDao.findDirectMessagesAwaitingOutbox(createdBefore)

    suspend fun findMessagesAwaitingReadReceipt(conversationId: String): List<UnreadIncomingMessageDto> =
        chatDao.findMessagesAwaitingReadReceipt(conversationId)

    suspend fun markReadReceiptSent(messageId: String): Int = chatDao.markReadReceiptSent(messageId)

    suspend fun findConversation(conversationId: String): ConversationEntity? =
        chatDao.findConversationById(conversationId)

    suspend fun upsertIncomingChatMessage(
        conversation: ConversationEntity,
        message: MessageEntity,
        text: String,
        timestamp: Long
    ) {
        chatDao.upsertIncomingChatMessage(
            conversation = conversation,
            message = message,
            text = text,
            timestamp = timestamp
        )
    }
}
