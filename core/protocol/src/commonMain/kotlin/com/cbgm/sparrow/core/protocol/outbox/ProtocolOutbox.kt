package com.cbgm.sparrow.core.protocol.outbox

import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import kotlinx.coroutines.flow.Flow

interface ProtocolOutbox {
    /**
     * Adds a protocol packet to the persistent outgoing queue.
     *
     * Re-enqueuing the same packetId must not create a duplicate.
     */
    suspend fun enqueue(
        contactId: String,
        packet: SparrowPacket
    ): Result<ProtocolOutboxItem>

    fun observePending(): Flow<List<ProtocolOutboxItem>>

    suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>>

    suspend fun markProcessing(itemId: String): Result<Unit>

    suspend fun markSent(itemId: String): Result<Unit>

    suspend fun markSent(
        itemId: String,
        expiresAtEpochMilliseconds: Long
    ): Result<Unit> = markSent(itemId)

    fun observeNextSentExpiry(): Flow<Long?> = kotlinx.coroutines.flow.flowOf(null)

    suspend fun expireSent(nowEpochMilliseconds: Long): Result<List<ProtocolOutboxItem>> =
        Result.success(emptyList())

    suspend fun markFailed(
        itemId: String,
        errorMessage: String
    ): Result<Unit>

    suspend fun retry(itemId: String): Result<Unit>

    /**
     * Re-queues an already persisted packet for delivery without creating a duplicate row.
     *
     * Pending or currently processing packets are left untouched.
     */
    suspend fun resend(packetId: String): Result<Unit>

    suspend fun requeueInterrupted(): Result<Unit>

    suspend fun retryFailed(): Result<Unit>

    suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?>
}
