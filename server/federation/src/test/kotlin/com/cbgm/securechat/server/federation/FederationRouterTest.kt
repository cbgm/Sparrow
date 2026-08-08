package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.protocol.ClientRoute
import com.cbgm.securechat.server.protocol.ClientRoutingResult
import com.cbgm.securechat.server.protocol.DeliveryRoute
import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederatedTypingEvent
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FederationRouterTest {
    @Test
    fun offlineRecipientFallsBackToRecipientSelectedMailbox() =
        kotlinx.coroutines.test.runTest {
            var mailboxUsed = false
            val envelope = testEnvelope()
            val router =
                FederationRouter(
                    localNodeId = "node-a",
                    presenceDirectory = { ClientRoutingResult(it, emptyList()) },
                    nodeRegistry = { mailboxNodeDescriptor() },
                    localGateway = { error("Local gateway must not be used") },
                    remoteFederation = { _, _ -> error("Remote federation must not be used") },
                    mailbox = {
                        mailboxUsed = true
                        FederationAcknowledgement(it.envelopeId, EnvelopeAcceptanceState.STORED_AT_DESTINATION)
                    }
                )

            val acknowledgement = router.route(envelope)

            assertTrue(mailboxUsed)
            assertEquals(EnvelopeAcceptanceState.STORED_AT_DESTINATION, acknowledgement.state)
        }

    @Test
    fun queuedEnvelopeIsRetriedWhenItsBackoffIsDue() =
        kotlinx.coroutines.test.runTest {
            var currentTime = 1_000L
            var mailboxAttempts = 0
            val queue = OutboundEnvelopeQueue(now = { currentTime })
            val router =
                FederationRouter(
                    localNodeId = "node-a",
                    presenceDirectory = { ClientRoutingResult(it, emptyList()) },
                    nodeRegistry = { mailboxNodeDescriptor() },
                    localGateway = { error("Local gateway must not be used") },
                    remoteFederation = { _, _ -> error("Remote federation must not be used") },
                    mailbox = {
                        mailboxAttempts += 1
                        if (mailboxAttempts == 1) {
                            error("Mailbox temporarily unavailable")
                        }
                        FederationAcknowledgement(
                            it.envelopeId,
                            EnvelopeAcceptanceState.STORED_AT_DESTINATION
                        )
                    },
                    queue = queue,
                    retryBaseDelayMilliseconds = 500L,
                    retryMaximumDelayMilliseconds = 2_000L,
                    now = { currentTime }
                )

            assertEquals(EnvelopeAcceptanceState.QUEUED_AT_GATEWAY, router.route(testEnvelope()).state)
            assertEquals(1, router.pendingCount())
            currentTime = 1_499L
            assertEquals(0, router.retryPending(limit = 10))
            currentTime = 1_500L
            assertEquals(1, router.retryPending(limit = 10))
            assertEquals(2, mailboxAttempts)
            assertEquals(0, router.pendingCount())
        }

    @Test
    fun durablePushFallbackCompletesFederationQueue() =
        kotlinx.coroutines.test.runTest {
            val currentTime = 1_000L
            val queue = OutboundEnvelopeQueue(now = { currentTime })
            val router =
                FederationRouter(
                    localNodeId = "node-a",
                    presenceDirectory = { ClientRoutingResult(it, emptyList()) },
                    nodeRegistry = { null },
                    localGateway = { error("Local gateway must not be used") },
                    remoteFederation = { _, _ -> error("Remote federation must not be used") },
                    mailbox = { error("Mailbox route is unavailable") },
                    queue = queue,
                    now = { currentTime }
                )
            val envelope = testEnvelope().copy(mailboxRoute = null)

            assertEquals(EnvelopeAcceptanceState.QUEUED_AT_GATEWAY, router.route(envelope).state)
            assertEquals(1, router.pendingCount())

            router.markStored(envelope.envelopeId)

            assertEquals(0, router.pendingCount())
        }

    @Test
    fun typingEventIsRoutedToRemoteNodePresence() =
        kotlinx.coroutines.test.runTest {
            var deliveredEvent: FederatedTypingEvent? = null
            val event =
                FederatedTypingEvent(
                    senderRoutingId = "sender",
                    recipientRoutingId = "recipient",
                    isTyping = true
                )
            val router =
                FederationRouter(
                    localNodeId = "node-a",
                    presenceDirectory =
                        {
                            ClientRoutingResult(
                                routingId = it,
                                routes =
                                    listOf(
                                        ClientRoute(
                                            routingId = it,
                                            nodeId = "node-b",
                                            connectionId = "connection-b",
                                            generation = 1L,
                                            expiresAtEpochMilliseconds = 10_000L,
                                            clientSignature = byteArrayOf(1)
                                        )
                                    )
                            )
                        },
                    nodeRegistry = { testNodeDescriptor() },
                    localGateway = { error("Envelope gateway must not be used") },
                    remoteFederation = { _, _ -> error("Envelope federation must not be used") },
                    mailbox = { error("Mailbox must not be used") },
                    remoteTypingFederation =
                        { _, candidate ->
                            deliveredEvent = candidate
                            true
                        }
                )

            assertTrue(router.routeTyping(event))
            assertEquals(event, deliveredEvent)
        }

    @Test
    fun onlineEnvelopeUsesPeerRouteWhenControlPlaneIsUnavailable() =
        kotlinx.coroutines.test.runTest {
            var deliveredEnvelope: FederatedEnvelope? = null
            val peer = testNodeDescriptor()
            val peerClient =
                TestPeerFederationClient(
                    resolveRoute = { routingId ->
                        if (routingId == "recipient-alias") "recipient" else null
                    },
                    deliverEnvelope = { envelope ->
                        deliveredEnvelope = envelope
                        FederationAcknowledgement(
                            envelopeId = envelope.envelopeId,
                            state = EnvelopeAcceptanceState.STORED_AT_DESTINATION
                        )
                    }
                )
            val router =
                FederationRouter(
                    localNodeId = "node-a",
                    presenceDirectory = { error("Control plane unavailable") },
                    nodeRegistry = TestPeerNodeRegistry(peer),
                    localGateway = { error("Recipient is remote") },
                    remoteFederation = peerClient,
                    mailbox = { error("Mailbox must not be used") }
                )

            val acknowledgement =
                router.route(
                    testEnvelope().copy(
                        recipientDeviceRoutingId = "recipient-alias",
                        mailboxRoute = null
                    )
                )

            assertEquals(
                EnvelopeAcceptanceState.STORED_AT_DESTINATION,
                acknowledgement.state
            )
            assertEquals(
                "recipient",
                requireNotNull(deliveredEnvelope).recipientDeviceRoutingId
            )
        }

    @Test
    fun typingUsesPeerRouteWhenControlPlaneIsUnavailable() =
        kotlinx.coroutines.test.runTest {
            var deliveredEvent: FederatedTypingEvent? = null
            val peer = testNodeDescriptor()
            val peerClient =
                TestPeerFederationClient(
                    resolveRoute = { routingId ->
                        if (routingId == "recipient-alias") "recipient" else null
                    },
                    deliverTyping = { event ->
                        deliveredEvent = event
                        true
                    }
                )
            val router =
                FederationRouter(
                    localNodeId = "node-a",
                    presenceDirectory = { error("Control plane unavailable") },
                    nodeRegistry = TestPeerNodeRegistry(peer),
                    localGateway = { error("Envelope gateway must not be used") },
                    remoteFederation = peerClient,
                    mailbox = { error("Mailbox must not be used") },
                    remoteTypingFederation = peerClient
                )

            val delivered =
                router.routeTyping(
                    FederatedTypingEvent(
                        senderRoutingId = "sender",
                        recipientRoutingId = "recipient-alias",
                        isTyping = true
                    )
                )

            assertTrue(delivered)
            assertEquals("recipient", requireNotNull(deliveredEvent).recipientRoutingId)
        }

    private fun testEnvelope(): FederatedEnvelope =
        FederatedEnvelope(
            envelopeId = "envelope-1",
            senderRoutingId = "sender",
            recipientDeviceRoutingId = "recipient",
            mailboxRoute =
                DeliveryRoute(
                    routeId = "route",
                    nodeId = "mailbox-node",
                    nodeEndpoint = "http://mailbox",
                    mailboxId = "mailbox",
                    sendCapability = "capability",
                    sequence = 1L,
                    expiresAtEpochMilliseconds = 10_000L,
                    identitySignature = byteArrayOf(1)
                ),
            encryptedPayload = "ciphertext",
            createdAtEpochMilliseconds = 1_000L,
            expiresAtEpochMilliseconds = 9_000L
        )

    private fun testNodeDescriptor(): SecureChatNodeDescriptor =
        SecureChatNodeDescriptor(
            nodeId = "node-b",
            clientEndpoint = "ws://gateway-b/relay",
            federationEndpoint = "http://federation-b",
            mailboxEndpoint = "http://mailbox-b",
            identityPublicKey = byteArrayOf(1),
            protocolVersions = setOf(1),
            capabilities = NodeCapability.entries.toSet(),
            validUntilEpochMilliseconds = 10_000L,
            signature = byteArrayOf(1)
        )

    private fun mailboxNodeDescriptor(): SecureChatNodeDescriptor =
        testNodeDescriptor().copy(
            nodeId = "mailbox-node",
            mailboxEndpoint = "http://mailbox"
        )

    private class TestPeerNodeRegistry(
        private val peer: SecureChatNodeDescriptor
    ) : NodeRegistryClient,
        PeerNodeDirectory {
        override suspend fun find(nodeId: String): SecureChatNodeDescriptor? = null

        override suspend fun peers(): List<SecureChatNodeDescriptor> = listOf(peer)
    }

    private class TestPeerFederationClient(
        private val resolveRoute: suspend (String) -> String? = { null },
        private val deliverEnvelope:
            suspend (FederatedEnvelope) -> FederationAcknowledgement =
            { envelope ->
                FederationAcknowledgement(
                    envelopeId = envelope.envelopeId,
                    state = EnvelopeAcceptanceState.QUEUED_AT_GATEWAY
                )
            },
        private val deliverTyping: suspend (FederatedTypingEvent) -> Boolean = { false }
    ) : RemoteFederationClient,
        RemoteTypingFederationClient,
        RemoteRouteResolver {
        override suspend fun deliver(
            descriptor: SecureChatNodeDescriptor,
            envelope: FederatedEnvelope
        ): FederationAcknowledgement = deliverEnvelope(envelope)

        override suspend fun deliver(
            descriptor: SecureChatNodeDescriptor,
            event: FederatedTypingEvent
        ): Boolean = deliverTyping(event)

        override suspend fun resolve(
            descriptor: SecureChatNodeDescriptor,
            routingId: String
        ): String? = resolveRoute(routingId)
    }
}
