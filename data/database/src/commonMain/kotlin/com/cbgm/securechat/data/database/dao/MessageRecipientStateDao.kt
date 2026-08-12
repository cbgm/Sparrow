package com.cbgm.securechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.securechat.data.database.entity.MessageRecipientStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageRecipientStateDao {
    @Upsert
    suspend fun upsert(state: MessageRecipientStateEntity)

    @Upsert
    suspend fun upsertAll(states: List<MessageRecipientStateEntity>)

    @Query(
        """
        SELECT *
        FROM message_recipient_states
        WHERE messageId = :messageId
        ORDER BY contactId
        """
    )
    fun observeByMessageId(messageId: String): Flow<List<MessageRecipientStateEntity>>

    @Query(
        """
        SELECT *
        FROM message_recipient_states
        WHERE messageId = :messageId
        ORDER BY contactId
        """
    )
    suspend fun findByMessageId(messageId: String): List<MessageRecipientStateEntity>

    @Query(
        """
        SELECT *
        FROM message_recipient_states
        WHERE packetId = :packetId
        LIMIT 1
        """
    )
    suspend fun findByPacketId(packetId: String): MessageRecipientStateEntity?

    @Query(
        """
        SELECT message_recipient_states.*
        FROM message_recipient_states
        INNER JOIN messages ON messages.id = message_recipient_states.messageId
        WHERE messages.conversationId = :conversationId
        ORDER BY message_recipient_states.messageId, message_recipient_states.contactId
        """
    )
    fun observeByConversationId(conversationId: String): Flow<List<MessageRecipientStateEntity>>

    @Query(
        """
        SELECT message_recipient_states.*
        FROM message_recipient_states
        INNER JOIN messages ON messages.id = message_recipient_states.messageId
        WHERE messages.conversationId = :conversationId
          AND message_recipient_states.deliveryStatus = :deliveryStatus
          AND message_recipient_states.updatedAtEpochMilliseconds <= :updatedBeforeEpochMilliseconds
        ORDER BY message_recipient_states.messageId, message_recipient_states.contactId
        """
    )
    suspend fun findByConversationAndDeliveryStatusBefore(
        conversationId: String,
        deliveryStatus: String,
        updatedBeforeEpochMilliseconds: Long
    ): List<MessageRecipientStateEntity>

    @Query(
        """
        UPDATE message_recipient_states
        SET deliveryStatus = :deliveryStatus,
            lastError = :lastError,
            updatedAtEpochMilliseconds = :updatedAtEpochMilliseconds
        WHERE messageId = :messageId
          AND contactId = :contactId
        """
    )
    suspend fun updateDeliveryStatus(
        messageId: String,
        contactId: String,
        deliveryStatus: String,
        lastError: String?,
        updatedAtEpochMilliseconds: Long
    ): Int
}
