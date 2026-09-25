package com.cbgm.sparrow.feature.messaging.runtime.outbox

import com.cbgm.sparrow.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.sparrow.core.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxFailureEvent
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.protocol.transport.OutgoingWireAcceptance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultOutboxProcessorTest {
    @Test
    fun relayAcceptanceUpdatesOutboxBeforeNotifyingListener() = runTest {
        val outbox = RecordingOutbox()
        val listener = RecordingDeliveryListener(outbox)
        val processor = DefaultOutboxProcessor(
            protocolOutbox = outbox,
            send = { Result.success(OutgoingWireAcceptance(100_000L)) },
            deliveryStateListener = listener
        )
        val outcome = processor.processPending(20).getOrThrow()
        assertEquals(1, outcome.processedCount)
        assertEquals(1, outcome.sentCount)
        assertEquals(0, outcome.failedCount)
        assertEquals(listOf("processing", "sent"), listener.events)
        assertEquals(OutboxStatus.SENT, outbox.item.status)
        assertEquals(100_000L, outbox.item.expiresAtEpochMilliseconds)
    }

    @Test
    fun failedSendPersistsFailureWithoutCallingApplicationCode() = runTest {
        val outbox = RecordingOutbox()
        val listener = RecordingDeliveryListener(outbox)
        val processor = DefaultOutboxProcessor(
            protocolOutbox = outbox,
            send = { Result.failure(IllegalStateException("network down")) },
            deliveryStateListener = listener
        )
        val outcome = processor.processPending(20).getOrThrow()
        assertEquals(1, outcome.failedCount)
        assertEquals(listOf("processing", "failed"), listener.events)
        assertEquals("network down", outbox.item.lastError)
    }

    @Test
    fun invalidLimitDoesNotAccessOutbox() = runTest {
        val outbox = RecordingOutbox()
        val processor = DefaultOutboxProcessor(
            protocolOutbox = outbox,
            send = { Result.failure(IllegalStateException("unexpected")) },
            deliveryStateListener = RecordingDeliveryListener(outbox)
        )
        assertTrue(processor.processPending(0).isFailure)
        assertEquals(OutboxStatus.PENDING, outbox.item.status)
    }

    private class RecordingOutbox : ProtocolOutbox {
        override fun observeUnacknowledgedFailures(): Flow<List<ProtocolOutboxFailureEvent>> =
            kotlinx.coroutines.flow.flowOf(emptyList())

        override suspend fun acknowledgeFailure(eventId: String): Result<Unit> = Result.success(Unit)

        var item = ProtocolOutboxItem(
            id = "outbox-1",
            contactId = "contact-1",
            packetId = "packet-1",
            encodedPacket = byteArrayOf(1),
            status = OutboxStatus.PENDING,
            attemptCount = 0,
            lastError = null,
            createdAtEpochMilliseconds = 1,
            updatedAtEpochMilliseconds = 1
        )

        override suspend fun enqueue(contactId: String, packet: SparrowPacket): Result<ProtocolOutboxItem> =
            Result.failure(UnsupportedOperationException())

        override fun observePending(): Flow<List<ProtocolOutboxItem>> = flowOf(listOf(item))

        override fun observeTransportStates(): Flow<List<ProtocolOutboxItem>> = flowOf(emptyList())

        override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> =
            Result.success(if (item.status == OutboxStatus.PENDING) listOf(item) else emptyList())

        override suspend fun markProcessing(itemId: String): Result<Unit> {
            item = item.copy(status = OutboxStatus.PROCESSING, attemptCount = item.attemptCount + 1)
            return Result.success(Unit)
        }

        override suspend fun markSent(itemId: String): Result<Unit> {
            item = item.copy(status = OutboxStatus.SENT)
            return Result.success(Unit)
        }

        override suspend fun markSent(itemId: String, expiresAtEpochMilliseconds: Long): Result<Unit> {
            item = item.copy(status = OutboxStatus.SENT, expiresAtEpochMilliseconds = expiresAtEpochMilliseconds)
            return Result.success(Unit)
        }

        override suspend fun markFailed(itemId: String, errorMessage: String): Result<Unit> {
            item = item.copy(status = OutboxStatus.FAILED, lastError = errorMessage)
            return Result.success(Unit)
        }

        override suspend fun retry(itemId: String): Result<Unit> = Result.success(Unit)

        override suspend fun resend(packetId: String): Result<Unit> = Result.success(Unit)

        override suspend fun requeueInterrupted(): Result<Unit> = Result.success(Unit)

        override suspend fun retryFailed(): Result<Unit> = Result.success(Unit)

        override suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?> = Result.success(item)
    }

    private class RecordingDeliveryListener(
        private val outbox: RecordingOutbox
    ) : OutboxDeliveryStateListener {
        val events = mutableListOf<String>()

        override suspend fun onProcessing(packetId: String): Result<Unit> {
            assertEquals(OutboxStatus.PROCESSING, outbox.item.status)
            events += "processing"
            return Result.success(Unit)
        }

        override suspend fun onPrepared(packetId: String, encodedTransportPayload: String, transportMode: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun onSent(packetId: String): Result<Unit> {
            assertEquals(OutboxStatus.SENT, outbox.item.status)
            events += "sent"
            return Result.success(Unit)
        }

        override suspend fun onFailed(packetId: String, errorMessage: String): Result<Unit> {
            assertEquals(OutboxStatus.FAILED, outbox.item.status)
            events += "failed"
            return Result.success(Unit)
        }
    }
}
