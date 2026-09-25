package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxFailureEvent
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveMessagingFailureEventsUseCaseTest {
    @Test
    fun immutableFailureRemainsAfterRetryUntilExplicitAcknowledgement() = runTest {
        val outbox = FakeOutbox()
        outbox.failures.value = listOf(
            ProtocolOutboxFailureEvent(
                eventId = "row:2",
                packetId = "packet",
                encodedPacket = byteArrayOf(1, 2, 3),
                attemptCount = 2,
                errorMessage = "offline",
                occurredAtEpochMilliseconds = 123L
            )
        )

        // A mutable outbox packet may already have been retried; the attempt journal is independent.
        outbox.retried = true
        val first = ObserveMessagingFailureEventsUseCase(outbox)().first().single()
        assertEquals("row:2", first.eventId)
        assertEquals(2, first.attemptCount)
        assertContentEquals(byteArrayOf(1, 2, 3), first.encodedPacket)
        assertEquals("offline", first.errorMessage)
        assertTrue(outbox.retried)

        AcknowledgeMessagingFailureUseCase(outbox)(first.eventId).getOrThrow()
        assertTrue(ObserveMessagingFailureEventsUseCase(outbox)().first().isEmpty())
    }

    private class FakeOutbox : ProtocolOutbox {
        val failures = MutableStateFlow<List<ProtocolOutboxFailureEvent>>(emptyList())
        var retried = false

        override fun observeUnacknowledgedFailures(): Flow<List<ProtocolOutboxFailureEvent>> = failures

        override suspend fun acknowledgeFailure(eventId: String): Result<Unit> {
            failures.value = failures.value.filterNot { it.eventId == eventId }
            return Result.success(Unit)
        }

        override fun observePending(): Flow<List<ProtocolOutboxItem>> = flowOf(emptyList())

        override fun observeTransportStates(): Flow<List<ProtocolOutboxItem>> = flowOf(emptyList())

        override suspend fun enqueue(contactId: String, packet: SparrowPacket): Result<ProtocolOutboxItem> = error("unused")

        override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> = error("unused")

        override suspend fun markProcessing(itemId: String): Result<Unit> = error("unused")

        override suspend fun markSent(itemId: String): Result<Unit> = error("unused")

        override suspend fun markFailed(itemId: String, errorMessage: String): Result<Unit> = error("unused")

        override suspend fun retry(itemId: String): Result<Unit> = error("unused")

        override suspend fun resend(packetId: String): Result<Unit> = error("unused")

        override suspend fun requeueInterrupted(): Result<Unit> = error("unused")

        override suspend fun retryFailed(): Result<Unit> = error("unused")

        override suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?> = error("unused")
    }
}
