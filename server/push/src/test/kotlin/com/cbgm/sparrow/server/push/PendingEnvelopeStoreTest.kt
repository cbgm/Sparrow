package com.cbgm.sparrow.server.push

import com.cbgm.sparrow.server.protocol.TransportEnvelope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PendingEnvelopeStoreTest {
    @Test
    fun envelopeIsDeduplicatedAndRemovedOnlyAfterAcknowledgement() =
        runTest {
            val store = InMemoryPendingEnvelopeStore()
            val envelope =
                TransportEnvelope(
                    envelopeId = "envelope-1",
                    senderId = "sender",
                    recipientId = "recipient",
                    payload = "ciphertext",
                    createdAtEpochMilliseconds = 1L
                )

            assertTrue(store.enqueue(envelope))
            assertFalse(store.enqueue(envelope))
            assertEquals(listOf(envelope), store.pending("recipient"))
            assertEquals(setOf("recipient"), store.pendingRecipientIds())

            store.remove("recipient", envelope.envelopeId)

            assertEquals(emptyList(), store.pending("recipient"))
            assertEquals(emptySet(), store.pendingRecipientIds())
        }
}
