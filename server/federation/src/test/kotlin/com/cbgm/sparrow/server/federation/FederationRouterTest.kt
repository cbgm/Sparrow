package com.cbgm.sparrow.server.federation

import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRoutingResult
import com.cbgm.sparrow.server.protocol.DeliveryRoute
import com.cbgm.sparrow.server.protocol.EnvelopeAcceptanceState
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederatedTypingEvent
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.NodeCapability
import com.cbgm.sparrow.server.protocol.SparrowNodeDescriptor
import kotlinx.coroutines.async
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FederationRouterTest {
    @Test
    fun retryFallsBackToRecipientSelectedMailbox() =
        kotlinx.coroutines.test.runTest {
            var currentTime = 1_000L
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
                        FederationAcknowledgement(
                            it.envelopeId,
                            EnvelopeAcceptanceState.STORED_AT_DESTINATION
                        )
                    },
                    queue = OutboundEnvelopeQueue(now = { currentTime }),
                    retryBaseDelayMilliseconds = 500L,
                    now = { currentTime }
                )

            val acknowledgement = router.route(envelope)

            assertEquals(EnvelopeAcceptanceState.QUEUED_AT_GATEWAY, acknowledgement.state)
            assertFalse(mailboxUsed)
            currentTime = 1_500L
            assertEquals(1, router.retryPending(limit = 10))
            assertTrue(mailboxUsed)
            assertEquals(0, router.pendingCount())
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
            assertEquals(0, mailboxAttempts)
            assertEquals(1, router.pendingCount())
            currentTime = 1_499L
            assertEquals(0, router.retryPending(limit = 10))
            currentTime = 1_500L
            assertEquals(1, router.retryPending(limit = 10))
            assertEquals(1, mailboxAttempts)
            assertEquals(1, router.pendingCount())
            currentTime = 2_500L
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
    fun differentEnvelopesAreDeliveredConcurrently() =
        kotlinx.coroutines.test.runTest {
            val firstStarted = kotlinx.coroutines.CompletableDeferred<Unit>()
            val releaseFirst = kotlinx.coroutines.CompletableDeferred<Unit>()
            val localGateway =
                object : LocalGatewayClient, LocalRouteResolver {
                    override suspend fun resolve(routingId: String): String = routingId

                    override suspend fun deliver(envelope: FederatedEnvelope): FederationAcknowledgement {
                        if (envelope.envelopeId == "envelope-1") {
                            firstStarted.complete(Unit)
                            releaseFirst.await()
                        }
                        return FederationAcknowledgement(
                            envelope.envelopeId,
                            EnvelopeAcceptanceState.STORED_AT_DESTINATION
                        )
                    }
                }
            val router =
                FederationRouter(
                    localNodeId = "node-a",
                    presenceDirectory = { error("Presence must not be used") },
                    nodeRegistry = { null },
                    localGateway = localGateway,
                    remoteFederation = { _, _ -> error("Remote federation must not be used") },
                    mailbox = { error("Mailbox must not be used") }
                )

            val first = async { router.route(testEnvelope()) }
            firstStarted.await()

            val second =
                kotlinx.coroutines.withTimeout(1_000L) {
                    router.route(testEnvelope().copy(envelopeId = "envelope-2"))
                }

            assertEquals(EnvelopeAcceptanceState.STORED_AT_DESTINATION, second.state)
            releaseFirst.complete(Unit)
            assertEquals(EnvelopeAcceptanceState.STORED_AT_DESTINATION, first.await().state)
        }

    @Test
    fun sameEnvelopeIsNotDeliveredConcurrently() =
        kotlinx.coroutines.test.runTest {
            val firstStarted = kotlinx.coroutines.CompletableDeferred<Unit>()
            val releaseFirst = kotlinx.coroutines.CompletableDeferred<Unit>()
            var deliveries = 0
            val localGateway =
                object : LocalGatewayClient, LocalRouteResolver {
                    override suspend fun resolve(routingId: String): String = routingId

                    override suspend fun deliver(envelope: FederatedEnvelope): FederationAcknowledgement {
                        deliveries += 1
                        firstStarted.complete(Unit)
                        releaseFirst.await()
                        return FederationAcknowledgement(
                            envelope.envelopeId,
                            EnvelopeAcceptanceState.STORED_AT_DESTINATION
                        )
                    }
                }
            val router =
                FederationRouter(
                    localNodeId = "node-a",
                    presenceDirectory = { error("Presence must not be used") },
                    nodeRegistry = { null },
                    localGateway = localGateway,
                    remoteFederation = { _, _ -> error("Remote federation must not be used") },
                    mailbox = { error("Mailbox must not be used") }
                )
            val envelope = testEnvelope()

            val first = async { router.route(envelope) }
            firstStarted.await()
            val duplicate = router.route(envelope)

            assertTrue(duplicate.duplicate)
            assertEquals(EnvelopeAcceptanceState.QUEUED_AT_GATEWAY, duplicate.state)
            assertEquals(1, deliveries)

            releaseFirst.complete(Unit)
            assertEquals(EnvelopeAcceptanceState.STORED_AT_DESTINATION, first.await().state)
            assertEquals(1, deliveries)
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
    fun initialRouteDoesNotWaitForControlPlaneFallback() =
        kotlinx.coroutines.test.runTest {
            var controlPlaneCalls = 0
            val router =
                FederationRouter(
                    localNodeId = "node-a",
                    presenceDirectory = { routingId ->
                        controlPlaneCalls += 1
                        ClientRoutingResult(routingId, emptyList())
                    },
                    nodeRegistry = TestPeerNodeRegistry(testNodeDescriptor()),
                    localGateway = { error("Recipient is remote") },
                    remoteFederation =
                        TestPeerFederationClient(
                            resolveRoute = { null }
                        ),
                    mailbox = { error("Mailbox must not run on initial route") }
                )

            val acknowledgement =
                router.route(
                    testEnvelope().copy(mailboxRoute = null)
                )

            assertEquals(EnvelopeAcceptanceState.QUEUED_AT_GATEWAY, acknowledgement.state)
            assertEquals(0, controlPlaneCalls)
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

    private fun testNodeDescriptor(): SparrowNodeDescriptor =
        SparrowNodeDescriptor(
            nodeId = "node-b",
            clientEndpoint = "ws://gateway-b/v1/gateway",
            federationEndpoint = "http://federation-b",
            mailboxEndpoint = "http://mailbox-b",
            identityPublicKey = byteArrayOf(1),
            protocolVersions = setOf(1),
            capabilities = NodeCapability.entries.toSet(),
            validUntilEpochMilliseconds = 10_000L,
            signature = byteArrayOf(1)
        )

    private fun mailboxNodeDescriptor(): SparrowNodeDescriptor =
        testNodeDescriptor().copy(
            nodeId = "mailbox-node",
            mailboxEndpoint = "http://mailbox"
        )

    private class TestPeerNodeRegistry(
        private val peer: SparrowNodeDescriptor
    ) : NodeRegistryClient,
        PeerNodeDirectory {
        override suspend fun find(nodeId: String): SparrowNodeDescriptor? = null

        override suspend fun peers(): List<SparrowNodeDescriptor> = listOf(peer)
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
            descriptor: SparrowNodeDescriptor,
            envelope: FederatedEnvelope
        ): FederationAcknowledgement = deliverEnvelope(envelope)

        override suspend fun deliver(
            descriptor: SparrowNodeDescriptor,
            event: FederatedTypingEvent
        ): Boolean = deliverTyping(event)

        override suspend fun resolve(
            descriptor: SparrowNodeDescriptor,
            routingId: String
        ): String? = resolveRoute(routingId)
    }
}
