package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.core.protocol.transport.OutgoingWireAcceptance
import com.cbgm.sparrow.core.protocol.transport.OutgoingWireSender
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SendEncodedTransportUseCaseTest {
    @Test
    fun forwardsPreResolvedEnvelopeWithoutModification() =
        runTest {
            val wire = FakeOutgoingWireSender()
            val result = SendEncodedTransportUseCase(wire)("routing-1", "encoded-1")

            assertEquals(1, wire.callCount)
            assertEquals("routing-1", wire.lastRoutingId)
            assertEquals("encoded-1", wire.lastPayload)
            assertEquals(1234L, result.getOrThrow().expiresAtEpochMilliseconds)
        }

    @Test
    fun preservesTransportFailure() =
        runTest {
            val wire = FakeOutgoingWireSender(fail = true)
            val result = SendEncodedTransportUseCase(wire)("routing-1", "encoded-1")

            assertTrue(result.isFailure)
            assertEquals("transport failed", result.exceptionOrNull()?.message)
            assertEquals(1, wire.callCount)
        }

    @Test
    fun doesNotSendWithBlankRecipient() =
        runTest {
            val wire = FakeOutgoingWireSender()
            assertTrue(SendEncodedTransportUseCase(wire)(" ", "encoded-1").isFailure)
            assertEquals(0, wire.callCount)
        }

    @Test
    fun doesNotSendWithBlankPayload() =
        runTest {
            val wire = FakeOutgoingWireSender()
            assertTrue(SendEncodedTransportUseCase(wire)("routing-1", " ").isFailure)
            assertEquals(0, wire.callCount)
        }

    private class FakeOutgoingWireSender(
        private val fail: Boolean = false
    ) : OutgoingWireSender {
        var callCount = 0
        var lastRoutingId: String? = null
        var lastPayload: String? = null

        override suspend fun send(
            recipientAddress: String,
            encodedTransportPayload: String
        ): Result<Unit> = Result.success(Unit)

        override suspend fun sendWithAcceptance(
            recipientAddress: String,
            encodedTransportPayload: String
        ): Result<OutgoingWireAcceptance> {
            callCount++
            lastRoutingId = recipientAddress
            lastPayload = encodedTransportPayload
            return if (fail) {
                Result.failure(IllegalStateException("transport failed"))
            } else {
                Result.success(OutgoingWireAcceptance(1234L))
            }
        }
    }
}
