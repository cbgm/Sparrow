package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface MessageDeliveryStatusDao {
    @Query(
        """
        SELECT deliveryStatus
        FROM messages
        WHERE packetId = :packetId
          AND isMine = 1
        LIMIT 1
        """
    )
    suspend fun findOutgoingDeliveryStatusByPacketId(packetId: String): String?

    @Query(
        """
        SELECT messages.deliveryStatus
        FROM messages
        INNER JOIN conversations
            ON conversations.id = messages.conversationId
        WHERE messages.id = :messageId
          AND messages.isMine = 1
          AND conversations.contactId = :contactId
        LIMIT 1
        """
    )
    suspend fun findOutgoingDeliveryStatus(
        messageId: String,
        contactId: String
    ): String?

    @Query(
        """
        SELECT deliveryStatus
        FROM messages
        WHERE id = :messageId
          AND isMine = 1
        LIMIT 1
        """
    )
    suspend fun findOutgoingDeliveryStatusByMessageId(messageId: String): String?

    @Query(
        """
        UPDATE messages
        SET deliveryStatus = :deliveryStatus
        WHERE packetId = :packetId
        """
    )
    suspend fun updateDeliveryStatus(
        packetId: String,
        deliveryStatus: String
    ): Int

    @Query(
        """
        UPDATE messages
        SET transportPayload = :transportPayload,
            transportMode = :transportMode
        WHERE packetId = :packetId
        """
    )
    suspend fun updatePreparedTransport(
        packetId: String,
        transportPayload: String,
        transportMode: String
    ): Int

    @Query(
        """
    UPDATE messages
    SET deliveryStatus = :deliveryStatus
    WHERE id = :messageId
    """
    )
    suspend fun updateDeliveryStatusByMessageId(
        messageId: String,
        deliveryStatus: String
    ): Int
}
