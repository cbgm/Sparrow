package com.cbgm.sparrow.feature.membership.data.security

import com.cbgm.sparrow.data.database.transaction.DatabaseTransactionRunner
import com.cbgm.sparrow.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.outbox.ProtocolOutboxFailureEvent
import com.cbgm.sparrow.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.protocol.packet.SparrowPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Runs the block directly and records whether code ran inside it. */
internal class RecordingTransactions : DatabaseTransactionRunner {
    var active = false
        private set

    override suspend fun <T> inTransaction(block: suspend () -> T): T {
        active = true
        try {
            return block()
        } finally {
            active = false
        }
    }
}

/** Records every queued packet and whether it was queued inside [transactions]. */
internal class RecordingOutbox(
    private val transactions: RecordingTransactions
) : ProtocolOutbox {
    val queued = mutableListOf<Pair<String, Boolean>>()

    override suspend fun enqueue(contactId: String, packet: SparrowPacket): Result<ProtocolOutboxItem> {
        queued += contactId to transactions.active
        return Result.success(
            ProtocolOutboxItem(
                id = "item-${packet.packetId}",
                contactId = contactId,
                packetId = packet.packetId,
                encodedPacket = byteArrayOf(1),
                status = OutboxStatus.PENDING,
                attemptCount = 0,
                lastError = null,
                createdAtEpochMilliseconds = 1,
                updatedAtEpochMilliseconds = 1
            )
        )
    }

    override fun observeUnacknowledgedFailures(): Flow<List<ProtocolOutboxFailureEvent>> = flowOf(emptyList())

    override suspend fun acknowledgeFailure(eventId: String): Result<Unit> = Result.success(Unit)

    override fun observePending(): Flow<List<ProtocolOutboxItem>> = flowOf(emptyList())

    override fun observeTransportStates(): Flow<List<ProtocolOutboxItem>> = flowOf(emptyList())

    override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> = Result.success(emptyList())

    override suspend fun markProcessing(itemId: String): Result<Unit> = Result.success(Unit)

    override suspend fun markSent(itemId: String): Result<Unit> = Result.success(Unit)

    override suspend fun markFailed(itemId: String, errorMessage: String): Result<Unit> = Result.success(Unit)

    override suspend fun retry(itemId: String): Result<Unit> = Result.success(Unit)

    override suspend fun resend(packetId: String): Result<Unit> = Result.success(Unit)

    override suspend fun requeueInterrupted(): Result<Unit> = Result.success(Unit)

    override suspend fun retryFailed(): Result<Unit> = Result.success(Unit)

    override suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?> = Result.success(null)
}
