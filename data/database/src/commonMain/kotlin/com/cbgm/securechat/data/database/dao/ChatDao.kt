package com.cbgm.securechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.entity.MessageRecipientStateEntity
import com.cbgm.securechat.data.database.model.ConversationSummary
import com.cbgm.securechat.data.database.model.ConversationWithMessages
import com.cbgm.securechat.data.database.model.UnreadIncomingMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query(
        """
        SELECT *
        FROM conversations
        WHERE contactId = :contactId
        LIMIT 1
        """
    )
    suspend fun findConversationByContactId(contactId: String): ConversationEntity?

    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Upsert
    suspend fun upsertConversationParticipant(participant: ConversationParticipantEntity)

    @Upsert
    suspend fun upsertConversationParticipants(participants: List<ConversationParticipantEntity>)

    @Query("DELETE FROM conversation_participants WHERE conversationId = :conversationId")
    suspend fun deleteConversationParticipants(conversationId: String)

    @Query(
        """
        DELETE FROM conversation_participants
        WHERE conversationId = :conversationId
          AND contactId = :contactId
        """
    )
    suspend fun deleteConversationParticipant(
        conversationId: String,
        contactId: String
    )

    @Query(
        """
        UPDATE conversation_participants
        SET role = :role
        WHERE conversationId = :conversationId
          AND contactId = :contactId
        """
    )
    suspend fun updateConversationParticipantRole(
        conversationId: String,
        contactId: String,
        role: String
    ): Int

    @Transaction
    suspend fun replaceConversationParticipants(
        conversationId: String,
        participants: List<ConversationParticipantEntity>
    ) {
        deleteConversationParticipants(conversationId)
        if (participants.isNotEmpty()) {
            upsertConversationParticipants(participants)
        }
    }

    @Transaction
    suspend fun replaceConversationParticipantsWithMessages(
        conversationId: String,
        participants: List<ConversationParticipantEntity>,
        messages: List<MessageEntity>
    ) {
        deleteConversationParticipants(conversationId)
        if (participants.isNotEmpty()) {
            upsertConversationParticipants(participants)
        }
        messages.forEach { message -> upsertMessage(message) }
    }

    @Transaction
    suspend fun applyLocalGroupRemoval(message: MessageEntity) {
        upsertMessage(message)
        deleteConversationParticipants(message.conversationId)
        updateConversationTimestamp(
            conversationId = message.conversationId,
            timestamp = message.createdAtEpochMilliseconds
        )
    }

    @Transaction
    suspend fun createGroupConversation(
        conversation: ConversationEntity,
        participants: List<ConversationParticipantEntity>
    ) {
        upsertConversation(conversation)
        upsertConversationParticipants(participants)
    }

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    fun observeConversationById(conversationId: String): Flow<ConversationEntity?>

    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    fun observeConversationWithMessagesById(conversationId: String): Flow<ConversationWithMessages?>

    @Query("SELECT * FROM conversation_participants WHERE conversationId = :conversationId")
    fun observeConversationParticipants(conversationId: String): Flow<List<ConversationParticipantEntity>>

    @Query("SELECT * FROM conversation_participants WHERE conversationId = :conversationId")
    suspend fun findConversationParticipants(conversationId: String): List<ConversationParticipantEntity>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun findConversationById(conversationId: String): ConversationEntity?

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM messages
            WHERE conversationId = :conversationId
              AND transportMode = :transportMode
        )
        """
    )
    suspend fun hasMessageWithTransportMode(
        conversationId: String,
        transportMode: String
    ): Boolean

    @Query(
        """
        SELECT createdAtEpochMilliseconds
        FROM messages
        WHERE conversationId = :conversationId
          AND transportMode = :transportMode
        LIMIT 1
        """
    )
    suspend fun findMessageTimestampByTransportMode(
        conversationId: String,
        transportMode: String
    ): Long?

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM messages
            WHERE conversationId = :conversationId
        )
        """
    )
    suspend fun hasMessages(conversationId: String): Boolean

    @Upsert
    suspend fun upsertMessage(message: MessageEntity)

    @Upsert
    suspend fun upsertMessageRecipientStates(states: List<MessageRecipientStateEntity>)

    @Transaction
    suspend fun upsertOutgoingGroupMessage(
        message: MessageEntity,
        recipientStates: List<MessageRecipientStateEntity>,
        timestamp: Long
    ) {
        upsertMessage(message)
        upsertMessageRecipientStates(recipientStates)
        updateConversationTimestamp(message.conversationId, timestamp)
    }

    /**
     * Atomically creates/reuses the conversation and stores an incoming
     * message. This guarantees that the recipient's chat list can observe
     * the conversation as soon as the first message arrives.
     */
    @Transaction
    suspend fun upsertIncomingChatMessage(
        conversation: ConversationEntity,
        message: MessageEntity,
        timestamp: Long,
        participant: ConversationParticipantEntity? = null
    ) {
        upsertConversation(
            conversation = conversation
        )

        participant?.let { upsertConversationParticipant(it) }

        upsertMessage(
            message = message
        )

        updateConversationTimestamp(
            conversationId = conversation.id,
            timestamp = timestamp
        )
    }

    @Query(
        """
        UPDATE conversations
        SET updatedAtEpochMilliseconds = :timestamp
        WHERE id = :conversationId
        """
    )
    suspend fun updateConversationTimestamp(
        conversationId: String,
        timestamp: Long
    )

    @Transaction
    @Query(
        """
        SELECT *
        FROM conversations
        WHERE contactId = :contactId
        LIMIT 1
        """
    )
    fun observeConversationByContactId(contactId: String): Flow<ConversationWithMessages?>

    @Query(
        """
    SELECT
        conversations.id AS conversationId,
        conversations.contactId AS contactId,
        contacts.displayName AS contactName,
        conversations.type AS conversationType,
        conversations.title AS conversationTitle,
        (
            SELECT COUNT(*)
            FROM conversation_participants
            WHERE conversation_participants.conversationId = conversations.id
        ) AS participantCount,
        (
            SELECT messages.text
            FROM messages
            WHERE messages.conversationId = conversations.id
            ORDER BY messages.createdAtEpochMilliseconds DESC, messages.id DESC
            LIMIT 1
        ) AS lastMessageText,
        (
            SELECT messages.createdAtEpochMilliseconds
            FROM messages
            WHERE messages.conversationId = conversations.id
            ORDER BY messages.createdAtEpochMilliseconds DESC, messages.id DESC
            LIMIT 1
        ) AS lastMessageTimestamp,
        (
            SELECT COUNT(*)
            FROM messages
            WHERE messages.conversationId = conversations.id
              AND messages.isMine = 0
              AND messages.readReceiptSent = 0
              AND messages.contentStatus = 'READABLE'
        ) AS unreadCount,
        conversations.updatedAtEpochMilliseconds AS updatedAtEpochMilliseconds
    FROM conversations
    LEFT JOIN contacts ON contacts.id = conversations.contactId
    WHERE (
        conversations.type = 'GROUP'
        OR EXISTS (
            SELECT 1
            FROM messages
            WHERE messages.conversationId = conversations.id
        )
        OR (
            conversations.type = 'DIRECT'
            AND conversations.contactId IS NOT NULL
            AND (
                SELECT identity_invitations.state
                FROM identity_invitations
                WHERE identity_invitations.contactId = conversations.contactId
                  AND identity_invitations.state IN (
                      :directChatAuthorizedState,
                      :directChatDeletedState
                  )
                ORDER BY
                    identity_invitations.updatedAtEpochMilliseconds DESC,
                    identity_invitations.createdAtEpochMilliseconds DESC
                LIMIT 1
            ) = :directChatAuthorizedState
        )
    )
      AND NOT EXISTS (
        SELECT 1
        FROM messages
        WHERE messages.conversationId = conversations.id
          AND messages.transportMode = :localDeletionTransportMode
    )
    ORDER BY conversations.updatedAtEpochMilliseconds DESC
    """
    )
    fun observeConversationSummaries(
        localDeletionTransportMode: String,
        directChatAuthorizedState: String,
        directChatDeletedState: String
    ): Flow<List<ConversationSummary>>

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteConversationMessages(conversationId: String)

    @Transaction
    suspend fun hideGroupConversation(marker: MessageEntity) {
        deleteConversationMessages(marker.conversationId)
        deleteConversationParticipants(marker.conversationId)
        upsertMessage(marker)
        updateConversationTimestamp(
            conversationId = marker.conversationId,
            timestamp = marker.createdAtEpochMilliseconds
        )
    }

    @Query(
        """
        DELETE FROM conversations
        WHERE id = :conversationId
        """
    )
    suspend fun deleteConversation(conversationId: String)

    @Query(
        """
    SELECT *
    FROM messages
    WHERE id = :messageId
    LIMIT 1
    """
    )
    suspend fun findMessageById(messageId: String): MessageEntity?

    @Query(
        """
        SELECT messages.*
        FROM messages
        INNER JOIN conversations
            ON conversations.id = messages.conversationId
        WHERE messages.conversationId = :conversationId
          AND conversations.type = 'GROUP'
          AND messages.isMine = 1
          AND messages.packetId IS NULL
          AND messages.deliveryStatus = 'QUEUED'
          AND NOT EXISTS (
              SELECT 1
              FROM message_recipient_states
              WHERE message_recipient_states.messageId = messages.id
          )
        ORDER BY messages.createdAtEpochMilliseconds, messages.id
        """
    )
    suspend fun findQueuedGroupMessages(conversationId: String): List<MessageEntity>

    @Query(
        """
    SELECT
        messages.id AS messageId,
        messages.conversationId AS conversationId,
        COALESCE(messages.senderContactId, conversations.contactId) AS contactId
    FROM messages
    INNER JOIN conversations
        ON conversations.id = messages.conversationId
    WHERE messages.conversationId = :conversationId
      AND messages.isMine = 0
      AND messages.readReceiptSent = 0
      AND messages.contentStatus = 'READABLE'
    ORDER BY messages.createdAtEpochMilliseconds ASC
    """
    )
    suspend fun findMessagesAwaitingReadReceipt(conversationId: String): List<UnreadIncomingMessage>

    @Query(
        """
    UPDATE messages
    SET readReceiptSent = 1
    WHERE id = :messageId
      AND isMine = 0
    """
    )
    suspend fun markReadReceiptSent(messageId: String): Int
}
