package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.MessageReactionDao
import com.cbgm.sparrow.data.database.dao.MessageRecipientStateDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import com.cbgm.sparrow.data.database.model.UnreadIncomingMessageDto

/** Keeps group-message persistence in the Chats datasource layer. */
class GroupOutgoingMessageDataSource(
    private val chatDao: ChatDao,
    private val messageReactionDao: MessageReactionDao,
    private val messageRecipientStateDao: MessageRecipientStateDao
) {
    suspend fun findMessage(messageId: String): MessageEntity? = chatDao.findMessageById(messageId)

    suspend fun findConversation(groupId: String): ConversationEntity? = chatDao.findConversationById(groupId)

    suspend fun findConversationParticipants(groupId: String): List<ConversationParticipantEntity> =
        chatDao.findConversationParticipants(groupId)

    suspend fun hasMessageWithTransportMode(conversationId: String, transportMode: String): Boolean =
        chatDao.hasMessageWithTransportMode(conversationId, transportMode)

    suspend fun findAwaitingReadReceipt(groupId: String): List<UnreadIncomingMessageDto> =
        chatDao.findMessagesAwaitingReadReceipt(groupId)

    suspend fun markReadReceiptSent(messageId: String): Int = chatDao.markReadReceiptSent(messageId)

    suspend fun saveMessage(message: MessageEntity) {
        chatDao.upsertMessage(message)
    }

    suspend fun saveOutgoingMessage(
        message: MessageEntity,
        recipientStates: List<MessageRecipientStateEntity>,
        timestamp: Long
    ) {
        chatDao.upsertOutgoingGroupMessage(message, recipientStates, timestamp)
    }

    suspend fun deleteMessages(messages: List<MessageEntity>) {
        chatDao.deleteMessagesAndRefreshConversations(messages)
    }

    suspend fun findReaction(messageId: String, reactorId: String, emoji: String): MessageReactionEntity? =
        messageReactionDao.find(messageId, reactorId, emoji)

    suspend fun deleteReaction(messageId: String, reactorId: String, emoji: String) {
        messageReactionDao.delete(messageId, reactorId, emoji)
    }

    suspend fun saveReaction(reaction: MessageReactionEntity) {
        messageReactionDao.upsert(reaction)
    }

    suspend fun findRecipientStates(messageId: String): List<MessageRecipientStateEntity> =
        messageRecipientStateDao.findByMessageId(messageId)

    suspend fun findRecipientByPacketId(packetId: String): MessageRecipientStateEntity? =
        messageRecipientStateDao.findByPacketId(packetId)
}
