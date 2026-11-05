package com.cbgm.sparrow.feature.transport.websocket

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class GatewayPendingRequestRegistryTest {
    @Test
    fun aMissingAcknowledgementIsAnOrdinaryFailure() =
        runTest {
            val result = GatewayPendingRequestRegistry().awaitEnvelopeAcceptance("envelope", 10L) {}

            val error = result.exceptionOrNull()
            assertIs<GatewayAcknowledgementTimeoutException>(error)
            assertFalse(error is CancellationException)
        }

    @Test
    fun aRefusedEnvelopeFailsItsSendAtOnce() =
        runTest {
            val registry = GatewayPendingRequestRegistry()
            val result =
                registry.awaitEnvelopeAcceptance("envelope", 60_000L) {
                    registry.rejectEnvelopeAcceptance("envelope", IllegalStateException("ENVELOPE_REJECTED: full"))
                }

            assertEquals("ENVELOPE_REJECTED: full", result.exceptionOrNull()?.message)
        }
}
