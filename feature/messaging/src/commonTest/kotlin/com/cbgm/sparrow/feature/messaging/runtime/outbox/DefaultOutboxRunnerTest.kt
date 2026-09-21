package com.cbgm.sparrow.feature.messaging.runtime.outbox

import com.cbgm.sparrow.core.protocol.outbox.OutboxProcessingResult
import com.cbgm.sparrow.core.protocol.outbox.OutboxProcessor
import com.cbgm.sparrow.core.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxFailureEvent
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

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
    fun startupDoesNotProcessPendingPacketsBeforeInterruptedRecovery() =
        runTest {
            val events = Channel<String>(capacity = Channel.UNLIMITED)
            val outbox = FakeProtocolOutbox(events)
            outbox.pending.value = listOf(createItem())
            val runner = DefaultOutboxRunner(
                protocolOutbox = outbox,
                outboxProcessor = RecordingOutboxProcessor(events)
            )
            try {
                runner.start()
                // First-start pending observation must not send a packet before
                // PROCESSING rows from the previous process have been reconciled.
                assertEquals(
                    listOf("requeue", "retry-failed", "process"),
                    receiveEvents(events, count = 3)
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
    fun reconnectStartRetriesWireFailuresWithoutRequeueingActivePackets() =
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

                // The first recovery event is recorded before that coroutine
                // has actually returned. Let its tail finish before testing a
                // separate connection notification.
                withContext(Dispatchers.Default) { kotlinx.coroutines.delay(50L.milliseconds) }
                runner.start()
                val reconnectEvents = receiveEvents(events, count = 2)

                assertEquals(listOf("requeue", "retry-failed", "process"), firstStartEvents)
                // A reconnect must NOT claim an existing in-flight packet as
                // interrupted and send it twice.
                assertEquals(listOf("retry-failed", "process"), reconnectEvents)
            } finally {
                runner.stop()
            }
        }

    @Test
    fun startAfterStopRecoversInterruptedPackets() =
        runTest {
            val events = Channel<String>(capacity = Channel.UNLIMITED)
            val outbox = FakeProtocolOutbox(events)
            val runner = DefaultOutboxRunner(
                protocolOutbox = outbox,
                outboxProcessor = RecordingOutboxProcessor(events)
            )
            try {
                runner.start()
                assertEquals(listOf("requeue", "retry-failed", "process"), receiveEvents(events, 3))
                runner.stop()
                runner.start()
                assertEquals(listOf("requeue", "retry-failed", "process"), receiveEvents(events, 3))
            } finally {
                runner.stop()
            }
        }

    @Test
    fun wireFailuresAreRetriedWithoutASecondConnectionEvent() =
        runTest {
            val events = Channel<String>(capacity = Channel.UNLIMITED)
            val outbox = FakeProtocolOutbox(events)
            val runner = DefaultOutboxRunner(
                protocolOutbox = outbox,
                outboxProcessor = RecordingOutboxProcessor(events),
                retryIntervalMilliseconds = 30L
            )
            try {
                runner.start()
                receiveEvents(events, count = 3)
                assertEquals(
                    "retry-transient",
                    withContext(Dispatchers.Default) {
                        withTimeout(2_000L.milliseconds) { events.receive() }
                    }
                )
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
        override fun observeUnacknowledgedFailures(): Flow<List<ProtocolOutboxFailureEvent>> =
            kotlinx.coroutines.flow.flowOf(emptyList())

        override suspend fun acknowledgeFailure(eventId: String): Result<Unit> = Result.success(Unit)

        val pending = MutableStateFlow<List<ProtocolOutboxItem>>(emptyList())

        override suspend fun enqueue(
            contactId: String,
            packet: SparrowPacket
        ): Result<ProtocolOutboxItem> = Result.failure(UnsupportedOperationException())

        override fun observePending(): Flow<List<ProtocolOutboxItem>> = pending

        override fun observeTransportStates(): Flow<List<ProtocolOutboxItem>> = kotlinx.coroutines.flow.flowOf(emptyList())

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

        override suspend fun retryTransientFailed(nowEpochMilliseconds: Long): Result<Unit> {
            events.send("retry-transient")
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
