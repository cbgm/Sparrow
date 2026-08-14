package com.cbgm.securechat.server.push

import com.cbgm.securechat.server.protocol.TransportEnvelope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PushCoordinatorTest {
    @Test
    fun duplicateEnvelopeIsAcceptedWhenItIsAlreadyDurable() =
        runTest {
            val pendingEnvelopes = InMemoryPendingEnvelopeStore()
            val coordinator =
                PushCoordinator(
                    pendingEnvelopes = pendingEnvelopes,
                    sender =
                        FirebasePushSender(
                            messaging = null,
                            devices = InMemoryPushDeviceStore(),
                            wakeUps = InMemoryWakeUpStore()
                        ),
                    scope = this
                )
            val envelope =
                TransportEnvelope(
                    envelopeId = "envelope-1",
                    senderId = "sender",
                    recipientId = "recipient",
                    payload = "ciphertext",
                    createdAtEpochMilliseconds = 1L
                )

            assertTrue(coordinator.accept(envelope))
            assertTrue(coordinator.accept(envelope))
            assertEquals(1, pendingEnvelopes.count())
        }

    @Test
    fun replicatedEnvelopeIsDurableWithoutChangingDuplicateSemantics() =
        runTest {
            val pendingEnvelopes = InMemoryPendingEnvelopeStore()
            val coordinator =
                PushCoordinator(
                    pendingEnvelopes = pendingEnvelopes,
                    sender =
                        FirebasePushSender(
                            messaging = null,
                            devices = InMemoryPushDeviceStore(),
                            wakeUps = InMemoryWakeUpStore()
                        ),
                    scope = this
                )
            val envelope =
                TransportEnvelope(
                    envelopeId = "envelope-replica",
                    senderId = "sender",
                    recipientId = "recipient",
                    payload = "ciphertext",
                    createdAtEpochMilliseconds = 1L
                )

            assertTrue(coordinator.replicate(envelope))
            assertTrue(coordinator.replicate(envelope))
            assertEquals(1, pendingEnvelopes.count())
        }
}
