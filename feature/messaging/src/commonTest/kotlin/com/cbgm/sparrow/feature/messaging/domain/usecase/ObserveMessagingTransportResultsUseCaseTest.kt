package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.core.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxFailureEvent
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.messaging.domain.model.MessagingTransportState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveMessagingTransportResultsUseCaseTest {
    @Test
    fun mapsPersistedStatusesWithoutCallingRelayAcceptanceDelivery() = runTest {
        val source = FakeOutbox()
        val observe = ObserveMessagingTransportResultsUseCase(source)
        source.states.value = listOf(
            item(OutboxStatus.SENT, "accepted"),
            item(OutboxStatus.FAILED, "failed"),
            item(OutboxStatus.EXPIRED, "expired"),
            item(OutboxStatus.PENDING, "pending")
        )
        val results = observe().first()
        assertEquals(3, results.size)
        assertEquals(MessagingTransportState.ACCEPTED_BY_RELAY, results[0].state)
        assertEquals(MessagingTransportState.FAILED, results[1].state)
        assertEquals(MessagingTransportState.EXPIRED, results[2].state)
        assertEquals("accepted", results[0].packetId)
        assertEquals(2, results[0].attemptCount)
    }

    private fun item(status: OutboxStatus, id: String) = ProtocolOutboxItem(
        id = id,
        contactId = "contact",
        packetId = id,
        encodedPacket = byteArrayOf(1),
        status = status,
        attemptCount = 2,
        lastError = null,
        createdAtEpochMilliseconds = 1,
        updatedAtEpochMilliseconds = 2
    )

    private class FakeOutbox : ProtocolOutbox {
        override fun observeUnacknowledgedFailures(): Flow<List<ProtocolOutboxFailureEvent>> =
            kotlinx.coroutines.flow.flowOf(emptyList())

        override suspend fun acknowledgeFailure(eventId: String): Result<Unit> = Result.success(Unit)

        val states = MutableStateFlow<List<ProtocolOutboxItem>>(emptyList())

        override fun observeTransportStates(): Flow<List<ProtocolOutboxItem>> = states

        override fun observePending(): Flow<List<ProtocolOutboxItem>> = flowOf(emptyList())

        override suspend fun enqueue(contactId: String, packet: SparrowPacket): Result<ProtocolOutboxItem> = error("unexpected")

        override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> = error("unexpected")

        override suspend fun markProcessing(itemId: String): Result<Unit> = error("unexpected")

        override suspend fun markSent(itemId: String): Result<Unit> = error("unexpected")

        override suspend fun markFailed(itemId: String, errorMessage: String): Result<Unit> = error("unexpected")

        override suspend fun retry(itemId: String): Result<Unit> = error("unexpected")

        override suspend fun resend(packetId: String): Result<Unit> = error("unexpected")

        override suspend fun requeueInterrupted(): Result<Unit> = error("unexpected")

        override suspend fun retryFailed(): Result<Unit> = error("unexpected")

        override suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?> = error("unexpected")
    }
}
