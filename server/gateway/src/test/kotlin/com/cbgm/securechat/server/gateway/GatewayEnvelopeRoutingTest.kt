package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederatedTypingEvent
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import com.cbgm.securechat.server.protocol.TransportEnvelope
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
    fun queuedEnvelopeIsAcceptedBeforePushFallbackCompletes() =
        runTest {
            var pushCalls = 0
            var scheduledEnvelope: TransportEnvelope? = null
            var scheduledEnvelopeId: String? = null
            val accepted =
                storeAndRouteFederatedEnvelope(
                    envelope = testEnvelope(),
                    pushStorage = {
                        pushCalls += 1
                        false
                    },
                    networkDelivery = { EnvelopeAcceptanceState.QUEUED_AT_GATEWAY },
                    markFederationStored = { error("Queued envelope must stay in federation") },
                    queuedPushFallback = { envelope, envelopeId ->
                        scheduledEnvelope = envelope
                        scheduledEnvelopeId = envelopeId
                    }
                )

            assertTrue(accepted)
            assertEquals(0, pushCalls)
            assertEquals("envelope-1", scheduledEnvelopeId)
            assertEquals("envelope-1", requireNotNull(scheduledEnvelope).envelopeId)
        }

    @Test
    fun remoteTypingEventIsPassedToFederation() =
        runTest {
            var routedEvent: FederatedTypingEvent? = null
            val event = testTypingEvent()
            val delivered =
                routeFederatedTypingEvent(
                    event = event,
                    localDelivery = { false },
                    federation =
                        federationClient(
                            typingDelegate = { candidate ->
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
        typingDelegate: suspend (FederatedTypingEvent) -> Boolean = { false },
        delegate: suspend (FederatedEnvelope) -> FederationAcknowledgement
    ): FederationClient =
        object : FederationClient {
            override suspend fun route(envelope: FederatedEnvelope): FederationAcknowledgement =
                delegate(envelope)

            override suspend fun routeTyping(event: FederatedTypingEvent): Boolean =
                typingDelegate(event)
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

    private fun testTypingEvent(): FederatedTypingEvent =
        FederatedTypingEvent(
            senderRoutingId = "sender",
            recipientRoutingId = "recipient",
            isTyping = true
        )
}
