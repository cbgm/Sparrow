package com.cbgm.sparrow.feature.messaging.application.outbox

import com.cbgm.sparrow.core.protocol.outbox.OutboxProcessingResult
import com.cbgm.sparrow.core.protocol.outbox.OutboxProcessor
import com.cbgm.sparrow.core.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultOutboxRunnerTest {
    @Test
    fun startRecoversInterruptedAndFailedItemsBeforeProcessing() =
        runTest {
            val events = Channel<String>(capacity = Channel.UNLIMITED)
            val outbox = FakeProtocolOutbox(events)
            val processor = RecordingOutboxProcessor(events)
            val runner =
                DefaultOutboxRunner(
                    protocolOutbox = outbox,
                    outboxProcessor = processor
                )

            try {
                runner.start()

                assertEquals(
                    expected = listOf("requeue", "retry-failed", "process"),
                    actual = receiveEvents(events, count = 3)
                )
            } finally {
                runner.stop()
            }
        }

    @Test
    fun pendingEmissionTriggersProcessing() =
        runTest {
            val events = Channel<String>(capacity = Channel.UNLIMITED)
            val outbox = FakeProtocolOutbox(events)
            val processor = RecordingOutboxProcessor(events)
            val runner =
                DefaultOutboxRunner(
                    protocolOutbox = outbox,
                    outboxProcessor = processor
                )

            try {
                runner.start()
                receiveEvents(events, count = 3)

                outbox.pending.value = listOf(createItem())

                assertEquals(
                    expected = "process",
                    actual = events.receive()
                )
            } finally {
                runner.stop()
            }
        }

    @Test
    fun reconnectStartRunsRecoveryAgain() =
        runTest {
            val events = Channel<String>(capacity = Channel.UNLIMITED)
            val outbox = FakeProtocolOutbox(events)
            val processor = RecordingOutboxProcessor(events)
            val runner =
                DefaultOutboxRunner(
                    protocolOutbox = outbox,
                    outboxProcessor = processor
                )

            try {
                runner.start()
                val firstStartEvents = receiveEvents(events, count = 3)

                runner.start()
                val reconnectEvents = receiveEvents(events, count = 3)

                assertEquals(listOf("requeue", "retry-failed", "process"), firstStartEvents)
                assertEquals(listOf("requeue", "retry-failed", "process"), reconnectEvents)
            } finally {
                runner.stop()
            }
        }

    private suspend fun receiveEvents(
        events: Channel<String>,
        count: Int
    ): List<String> =
        List(count) {
            events.receive()
        }

    private fun createItem(): ProtocolOutboxItem =
        ProtocolOutboxItem(
            id = "outbox-1",
            contactId = "contact-1",
            packetId = "packet-1",
            encodedPacket = byteArrayOf(1),
            status = OutboxStatus.PENDING,
            attemptCount = 0,
            lastError = null,
            createdAtEpochMilliseconds = 1L,
            updatedAtEpochMilliseconds = 1L
        )

    private class FakeProtocolOutbox(
        private val events: Channel<String>
    ) : ProtocolOutbox {
        val pending = MutableStateFlow<List<ProtocolOutboxItem>>(emptyList())

        override suspend fun enqueue(
            contactId: String,
            packet: SparrowPacket
        ): Result<ProtocolOutboxItem> = Result.failure(UnsupportedOperationException())

        override fun observePending(): Flow<List<ProtocolOutboxItem>> = pending

        override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> = Result.success(pending.value.take(limit))

        override suspend fun markProcessing(itemId: String): Result<Unit> = Result.failure(UnsupportedOperationException())

        override suspend fun markSent(itemId: String): Result<Unit> = Result.failure(UnsupportedOperationException())

        override suspend fun markFailed(
            itemId: String,
            errorMessage: String
        ): Result<Unit> = Result.failure(UnsupportedOperationException())

        override suspend fun retry(itemId: String): Result<Unit> = Result.failure(UnsupportedOperationException())

        override suspend fun resend(packetId: String): Result<Unit> = Result.success(Unit)

        override suspend fun requeueInterrupted(): Result<Unit> {
            events.send("requeue")
            return Result.success(Unit)
        }

        override suspend fun retryFailed(): Result<Unit> {
            events.send("retry-failed")
            return Result.success(Unit)
        }

        override suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?> = Result.success(null)
    }

    private class RecordingOutboxProcessor(
        private val events: Channel<String>
    ) : OutboxProcessor {
        override suspend fun processPending(limit: Int): Result<OutboxProcessingResult> {
            events.send("process")

            return Result.success(
                OutboxProcessingResult(
                    processedCount = 0,
                    sentCount = 0,
                    failedCount = 0
                )
            )
        }
    }
}
