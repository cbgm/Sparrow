package com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupMetadataDao
import com.cbgm.sparrow.data.database.dao.MessageRecipientStateDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.data.database.model.UnreadIncomingMessageDto

internal class GroupMessageDataSource(
    private val chatDao: ChatDao,
    private val messageRecipientStateDao: MessageRecipientStateDao,
    private val groupMetadataDao: GroupMetadataDao
) {
    suspend fun findConversation(conversationId: String): ConversationEntity? =
        chatDao.findConversationById(conversationId)

    suspend fun findTitle(groupId: String): String? = groupMetadataDao.get(groupId)?.title

    suspend fun hasMessageWithTransportMode(conversationId: String, transportMode: String): Boolean =
        chatDao.hasMessageWithTransportMode(conversationId, transportMode)

    suspend fun findMessage(messageId: String): MessageEntity? = chatDao.findMessageById(messageId)

    suspend fun findMessageText(messageId: String): String? = chatDao.findMessageText(messageId)

    suspend fun findQueuedGroupMessages(groupId: String): List<MessageEntity> = chatDao.findQueuedGroupMessages(groupId)

    suspend fun findGroupMessagesAwaitingOutbox(groupId: String): List<MessageEntity> =
        chatDao.findGroupMessagesAwaitingOutbox(groupId)

    suspend fun saveOutgoingMessage(
        message: MessageEntity,
        text: String,
        recipientStates: List<MessageRecipientStateEntity>,
        timestamp: Long
    ) {
        chatDao.upsertOutgoingGroupMessage(message, text, recipientStates, timestamp)
    }

    suspend fun saveRecipientStates(states: List<MessageRecipientStateEntity>) {
        if (states.isNotEmpty()) messageRecipientStateDao.upsertAll(states)
    }

    suspend fun findRecipientStates(messageId: String): List<MessageRecipientStateEntity> =
        messageRecipientStateDao.findByMessageId(messageId)

    suspend fun findRecipientByPacketId(packetId: String): MessageRecipientStateEntity? =
        messageRecipientStateDao.findByPacketId(packetId)

    suspend fun replaceMessageText(messageId: String, text: String) {
        chatDao.replaceMessageText(messageId, text)
    }

    suspend fun deleteMessages(messages: List<MessageEntity>) {
        chatDao.deleteMessagesAndRefreshConversations(messages)
    }

    suspend fun findAwaitingReadReceipt(groupId: String): List<UnreadIncomingMessageDto> =
        chatDao.findMessagesAwaitingReadReceipt(groupId)

    suspend fun markReadReceiptSent(messageId: String): Int = chatDao.markReadReceiptSent(messageId)

    suspend fun saveIncomingMessage(message: MessageEntity, text: String) {
        chatDao.upsertMessageWithText(message, text)
    }

    suspend fun updateConversationTimestamp(conversationId: String, timestamp: Long) {
        chatDao.updateConversationTimestamp(conversationId, timestamp)
    }
}
