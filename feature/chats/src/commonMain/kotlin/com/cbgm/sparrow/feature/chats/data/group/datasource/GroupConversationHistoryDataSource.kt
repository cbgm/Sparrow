package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.dao.MessageReactionDao
import com.cbgm.sparrow.data.database.dao.MessageRecipientStateDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.data.database.entity.MessageRecipientStateEntity
import kotlinx.coroutines.flow.Flow

/** Chats-owned group conversation, history, receipt, reaction and verification persistence. */
internal class GroupConversationHistoryDataSource(
    private val chatDao: ChatDao,
    private val messageRecipientStateDao: MessageRecipientStateDao,
    private val messageReactionDao: MessageReactionDao,
    private val groupVerificationDao: GroupVerificationDao
) {
    suspend fun upsertConversation(conversation: ConversationEntity) {
        chatDao.upsertConversation(conversation)
    }

    fun observeConversation(groupId: String): Flow<ConversationEntity?> =
        chatDao.observeConversationById(groupId)

    fun observeRecentMessages(groupId: String, limit: Int): Flow<List<MessageEntity>> =
        chatDao.observeRecentMessages(groupId, limit)

    fun observeMessagesFromCursor(
        conversationId: String,
        fromTimestamp: Long,
        fromMessageId: String
    ): Flow<List<MessageEntity>> =
        chatDao.observeMessagesFromCursor(conversationId, fromTimestamp, fromMessageId)

    fun observeMessagesByTransportModes(
        groupId: String,
        transportModes: List<String>
    ): Flow<List<MessageEntity>> =
        chatDao.observeMessagesByTransportModes(groupId, transportModes)

    fun observeRecentReactions(conversationId: String, messageLimit: Int): Flow<List<MessageReactionEntity>> =
        messageReactionDao.observeRecentByConversationId(conversationId, messageLimit)

    fun observeReactionsFromCursor(
        conversationId: String,
        fromTimestamp: Long,
        fromMessageId: String
    ): Flow<List<MessageReactionEntity>> =
        messageReactionDao.observeFromMessageCursor(conversationId, fromTimestamp, fromMessageId)

    fun observeRecentRecipientStates(
        conversationId: String,
        messageLimit: Int
    ): Flow<List<MessageRecipientStateEntity>> =
        messageRecipientStateDao.observeRecentByConversationId(conversationId, messageLimit)

    fun observeRecipientStatesFromCursor(
        conversationId: String,
        fromTimestamp: Long,
        fromMessageId: String
    ): Flow<List<MessageRecipientStateEntity>> =
        messageRecipientStateDao.observeFromMessageCursor(conversationId, fromTimestamp, fromMessageId)

    fun observeVerificationRows(groupId: String): Flow<List<GroupVerificationPairEntity>> =
        groupVerificationDao.observeByGroupId(groupId)

    suspend fun findVerificationRows(groupId: String): List<GroupVerificationPairEntity> =
        groupVerificationDao.findByGroupId(groupId)
}
