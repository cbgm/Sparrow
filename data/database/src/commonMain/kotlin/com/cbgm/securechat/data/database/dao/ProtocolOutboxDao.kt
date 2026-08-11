package com.cbgm.securechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.securechat.data.database.entity.ProtocolOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtocolOutboxDao {
    @Upsert
    suspend fun upsert(entity: ProtocolOutboxEntity)

    @Query(
        """
        SELECT *
        FROM protocol_outbox
        WHERE packetId = :packetId
        LIMIT 1
        """
    )
    suspend fun findByPacketId(packetId: String): ProtocolOutboxEntity?

    @Query(
        """
        SELECT *
        FROM protocol_outbox
        WHERE id = :itemId
        LIMIT 1
        """
    )
    suspend fun findById(itemId: String): ProtocolOutboxEntity?

    @Query(
        """
        SELECT *
        FROM protocol_outbox
        WHERE status = 'PENDING'
        ORDER BY createdAtEpochMilliseconds ASC
        """
    )
    fun observePending(): Flow<List<ProtocolOutboxEntity>>

    @Query(
        """
        SELECT *
        FROM protocol_outbox
        WHERE status = 'PENDING'
        ORDER BY createdAtEpochMilliseconds ASC
        LIMIT :limit
        """
    )
    suspend fun getPending(limit: Int): List<ProtocolOutboxEntity>

    @Query(
        """
        UPDATE protocol_outbox
        SET status = 'PROCESSING',
            attemptCount = attemptCount + 1,
            lastError = NULL,
            updatedAtEpochMilliseconds = :updatedAt
        WHERE id = :itemId
          AND status IN ('PENDING', 'FAILED')
        """
    )
    suspend fun markProcessing(
        itemId: String,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE protocol_outbox
        SET status = 'SENT',
            lastError = NULL,
            updatedAtEpochMilliseconds = :updatedAt
        WHERE id = :itemId
        """
    )
    suspend fun markSent(
        itemId: String,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE protocol_outbox
        SET status = 'FAILED',
            lastError = :errorMessage,
            updatedAtEpochMilliseconds = :updatedAt
        WHERE id = :itemId
        """
    )
    suspend fun markFailed(
        itemId: String,
        errorMessage: String,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE protocol_outbox
        SET status = 'PENDING',
            lastError = NULL,
            updatedAtEpochMilliseconds = :updatedAt
        WHERE id = :itemId
          AND status = 'FAILED'
        """
    )
    suspend fun retry(
        itemId: String,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE protocol_outbox
        SET status = 'PENDING',
            lastError = NULL,
            updatedAtEpochMilliseconds = :updatedAt
        WHERE packetId = :packetId
          AND status IN ('SENT', 'FAILED')
        """
    )
    suspend fun requeueForResend(
        packetId: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE protocol_outbox
        SET status = 'PENDING',
            lastError = NULL,
            updatedAtEpochMilliseconds = :updatedAt
        WHERE status = 'PROCESSING'
        """
    )
    suspend fun requeueInterrupted(updatedAt: Long)

    @Query(
        """
        UPDATE protocol_outbox
        SET status = 'PENDING',
            lastError = NULL,
            updatedAtEpochMilliseconds = :updatedAt
        WHERE status = 'FAILED'
        """
    )
    suspend fun retryFailed(updatedAt: Long)

    @Query(
        """
        DELETE FROM protocol_outbox
        WHERE status = 'SENT'
          AND updatedAtEpochMilliseconds < :beforeTimestamp
        """
    )
    suspend fun deleteSentBefore(beforeTimestamp: Long)
}
