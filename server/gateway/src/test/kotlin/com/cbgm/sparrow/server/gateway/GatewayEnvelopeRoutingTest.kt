package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.EnvelopeAcceptanceState
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederatedIndicatorEvent
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.TransportEnvelope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GatewayEnvelopeRoutingTest {
    @Test
    fun locallyConnectedRecipientSkipsFederation() =
        runTest {
            var federationCalls = 0
            val state =
                routeFederatedEnvelope(
                    envelope = testEnvelope(),
                    localDelivery = { true },
                    federation =
                        federationClient { envelope ->
                            federationCalls += 1
                            acknowledgement(
                                envelope = envelope,
                                state = EnvelopeAcceptanceState.STORED_AT_DESTINATION
                            )
                        }
                )

            assertEquals(EnvelopeAcceptanceState.STORED_AT_DESTINATION, state)
            assertEquals(0, federationCalls)
        }

    @Test
    fun remoteRecipientIsPassedToFederation() =
        runTest {
            var routedEnvelope: FederatedEnvelope? = null
            val envelope = testEnvelope()
            val state =
                routeFederatedEnvelope(
                    envelope = envelope,
                    localDelivery = { false },
                    federation =
                        federationClient { candidate ->
                            routedEnvelope = candidate
                            acknowledgement(
                                envelope = candidate,
                                state = EnvelopeAcceptanceState.STORED_AT_DESTINATION
                            )
                        }
                )

            assertEquals(EnvelopeAcceptanceState.STORED_AT_DESTINATION, state)
            assertEquals(envelope, routedEnvelope)
        }

    @Test
    fun onlineLegacyEnvelopeSkipsPushStorage() =
        runTest {
            var pushCalls = 0
            var networkCalls = 0
            var markedStoredEnvelopeId: String? = null
            val accepted =
                storeAndRouteLegacyEnvelope(
                    envelope = testTransportEnvelope(),
                    pushStorage = {
                        pushCalls += 1
                        true
                    },
                    networkDelivery = {
                        networkCalls += 1
                        EnvelopeAcceptanceState.STORED_AT_DESTINATION
                    },
                    markFederationStored = {
                        markedStoredEnvelopeId = it
                    }
                )

            assertTrue(accepted)
            assertEquals(0, pushCalls)
            assertEquals(1, networkCalls)
            assertEquals(null, markedStoredEnvelopeId)
        }

    @Test
    fun onlineFederatedEnvelopeSkipsPushStorage() =
        runTest {
            var pushedEnvelope: TransportEnvelope? = null
            var routedEnvelope: FederatedEnvelope? = null
            var markedStoredEnvelopeId: String? = null
            val envelope = testEnvelope()

            val accepted =
                storeAndRouteFederatedEnvelope(
                    envelope = envelope,
                    pushStorage = { candidate ->
                        pushedEnvelope = candidate
                        true
                    },
                    networkDelivery = { candidate ->
                        routedEnvelope = candidate
                        EnvelopeAcceptanceState.STORED_AT_DESTINATION
                    },
                    markFederationStored = { envelopeId ->
                        markedStoredEnvelopeId = envelopeId
                    }
                )

            assertTrue(accepted)
            assertEquals(null, pushedEnvelope)
            assertEquals(envelope, routedEnvelope)
            assertEquals(null, markedStoredEnvelopeId)
        }

    @Test
    fun federatedEnvelopePushFallbackAcceptsOfflineRecipient() =
        runTest {
            var markedStoredEnvelopeId: String? = null
            val accepted =
                storeAndRouteFederatedEnvelope(
                    envelope = testEnvelope(),
                    pushStorage = { true },
                    networkDelivery = { null },
                    markFederationStored = { envelopeId ->
                        markedStoredEnvelopeId = envelopeId
                    }
                )

            assertTrue(accepted)
            assertEquals("envelope-1", markedStoredEnvelopeId)
        }

    @Test
    fun durablePushFallbackAcceptsOfflineRecipientAndCompletesQueue() =
        runTest {
            var markedStoredEnvelopeId: String? = null
            val accepted =
                storeAndRouteLegacyEnvelope(
                    envelope = testTransportEnvelope(),
                    pushStorage = { true },
                    networkDelivery = { null },
                    markFederationStored = {
                        markedStoredEnvelopeId = it
                    }
                )

            assertTrue(accepted)
            assertEquals("envelope-1", markedStoredEnvelopeId)
        }

    @Test
    fun queuedEnvelopeIsAcceptedOnlyAfterOfflineInboxStoresIt() =
        runTest {
            var pushCalls = 0
            var storedEnvelope: TransportEnvelope? = null
            var markedStoredEnvelopeId: String? = null
            val accepted =
                storeAndRouteFederatedEnvelope(
                    envelope = testEnvelope(),
                    pushStorage = { envelope ->
                        pushCalls += 1
                        storedEnvelope = envelope
                        true
                    },
                    networkDelivery = { EnvelopeAcceptanceState.QUEUED_AT_GATEWAY },
                    markFederationStored = { markedStoredEnvelopeId = it }
                )

            assertTrue(accepted)
            assertEquals(1, pushCalls)
            assertEquals("envelope-1", requireNotNull(storedEnvelope).envelopeId)
            assertEquals("envelope-1", markedStoredEnvelopeId)
        }

    @Test
    fun queuedEnvelopeIsNotAcknowledgedWhenOfflineInboxCannotStoreIt() =
        runTest {
            var markedStored = false
            val accepted =
                storeAndRouteFederatedEnvelope(
                    envelope = testEnvelope(),
                    pushStorage = { false },
                    networkDelivery = { EnvelopeAcceptanceState.QUEUED_AT_GATEWAY },
                    markFederationStored = { markedStored = true }
                )

            assertEquals(false, accepted)
            assertEquals(false, markedStored)
        }

    @Test
    fun offlineInboxFailureAlsoRejectsWhenFederationCannotRoute() =
        runTest {
            val accepted =
                storeAndRouteLegacyEnvelope(
                    envelope = testTransportEnvelope(),
                    pushStorage = { false },
                    networkDelivery = { null },
                    markFederationStored = { error("No envelope was stored") }
                )
            assertEquals(false, accepted)
        }

    @Test
    fun serverAssignsDeliveryDeadlineForEstablishedRecipient() {
        val now = 10_000L
        val envelope =
            testTransportEnvelope()
                .copy(recipientId = "scrouting1_recipient")
                .toFederatedEnvelope(nowEpochMilliseconds = now)

        assertEquals(now + 7L * 24L * 60L * 60L * 1_000L, envelope.expiresAtEpochMilliseconds)
    }

    @Test
    fun serverAssignsShorterDeliveryDeadlineForBootstrapRecipient() {
        val now = 10_000L
        val envelope =
            testTransportEnvelope()
                .copy(recipientId = "scphone1_recipient")
                .toFederatedEnvelope(nowEpochMilliseconds = now)

        assertEquals(now + 24L * 60L * 60L * 1_000L, envelope.expiresAtEpochMilliseconds)
    }

    @Test
    fun remoteIndicatorEventIsPassedToFederation() =
        runTest {
            var routedEvent: FederatedIndicatorEvent? = null
            val event = testIndicatorEvent()
            val delivered =
                routeFederatedIndicatorEvent(
                    event = event,
                    localDelivery = { false },
                    federation =
                        federationClient(
                            indicatorDelegate = { candidate ->
                                routedEvent = candidate
                                true
                            }
                        ) { envelope ->
                            acknowledgement(
                                envelope = envelope,
                                state = EnvelopeAcceptanceState.QUEUED_AT_GATEWAY
                            )
                        }
                )

            assertTrue(delivered)
            assertEquals(event, routedEvent)
        }

    private fun federationClient(
        indicatorDelegate: suspend (FederatedIndicatorEvent) -> Boolean = { false },
        delegate: suspend (FederatedEnvelope) -> FederationAcknowledgement
    ): FederationClient =
        object : FederationClient {
            override suspend fun route(envelope: FederatedEnvelope): FederationAcknowledgement =
                delegate(envelope)

            override suspend fun routeIndicator(event: FederatedIndicatorEvent): Boolean =
                indicatorDelegate(event)
        }

    private fun acknowledgement(
        envelope: FederatedEnvelope,
        state: EnvelopeAcceptanceState
    ): FederationAcknowledgement =
        FederationAcknowledgement(
            envelopeId = envelope.envelopeId,
            state = state
        )

    private fun testEnvelope(): FederatedEnvelope =
        FederatedEnvelope(
            envelopeId = "envelope-1",
            senderRoutingId = "sender",
            recipientDeviceRoutingId = "recipient",
            mailboxRoute = null,
            encryptedPayload = "ciphertext",
            createdAtEpochMilliseconds = 1_000L,
            expiresAtEpochMilliseconds = 2_000L
        )

    private fun testTransportEnvelope(): TransportEnvelope =
        TransportEnvelope(
            envelopeId = "envelope-1",
            senderId = "sender",
            recipientId = "recipient",
            payload = "ciphertext",
            createdAtEpochMilliseconds = 1_000L
        )

    private fun testIndicatorEvent(): FederatedIndicatorEvent =
        FederatedIndicatorEvent(
            senderRoutingId = "sender",
            recipientRoutingId = "recipient",
            indicatorType = "TYPING"
        )
}
