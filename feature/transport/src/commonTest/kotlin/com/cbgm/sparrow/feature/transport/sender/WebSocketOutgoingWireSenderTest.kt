package com.cbgm.sparrow.feature.transport.sender

import com.cbgm.sparrow.feature.transport.circuit.CircuitCountStore
import com.cbgm.sparrow.feature.transport.circuit.CircuitFallbackStore
import com.cbgm.sparrow.feature.transport.circuit.CircuitHopCountStore
import com.cbgm.sparrow.feature.transport.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.circuit.DirectSendingPolicy
import com.cbgm.sparrow.feature.transport.circuit.ReadableCircuitCipher
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionState
import com.cbgm.sparrow.feature.transport.discovery.CachedNodeDirectory
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.gateway.model.TransportEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.InvalidInvitationLetterException
import com.cbgm.sparrow.feature.transport.mailbox.InvitationRouteMissingException
import com.cbgm.sparrow.feature.transport.mailbox.InvitationTestWorld
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.relay.RelaySelector
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WebSocketOutgoingWireSenderTest {
    @Test
    fun verifiedMailboxRouteUsesFederatedEnvelope() =
        runTest {
            val route =
                MailboxDeliveryRoute(
                    routeId = "route-1",
                    nodeId = "node-b",
                    nodeEndpoint = "https://node-b.example",
                    mailboxId = "mailbox-1",
                    sendCapability = "send-capability",
                    sequence = 4L,
                    expiresAtEpochMilliseconds = Long.MAX_VALUE,
                    identitySignature = byteArrayOf(1, 2, 3)
                )
            val client = RecordingWebSocketTransportClient()
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRoutingIdProvider = SuccessfulLocalRoutingIdProvider(),
                    localBootstrapRoutingIdProvider = SuccessfulLocalBootstrapRoutingIdProvider(),
                    transportConfig =
                        TransportConfig(),
                    mailboxRouteRepository = FakeMailboxRouteRepository(route)
                )

            assertTrue(sender.send("recipient-routing-id", "encoded-payload").isSuccess)

            val envelope = requireNotNull(client.federatedEnvelope)
            assertEquals(route, envelope.mailboxRoute)
            assertEquals("local-routing-id", envelope.senderRoutingId)
            assertEquals("recipient-routing-id", envelope.recipientDeviceRoutingId)
            assertEquals("encoded-payload", envelope.encryptedPayload)
            assertEquals(null, client.envelope)
        }

    @Test
    fun aContactWithAMailboxGetsTheEnvelopePutIntoIt() =
        runTest {
            val client = RecordingWebSocketTransportClient()
            val mailboxes = RecordingMailboxGateway()

            assertTrue(mailboxSender(client, mailboxes, sendsDirectly = true).send("recipient-routing-id", "encoded-payload").isSuccess)

            assertEquals(ROUTE, mailboxes.delivered?.mailboxRoute)
            assertEquals("encoded-payload", mailboxes.delivered?.encryptedPayload)
            // The mailbox and the nodes on the way do not learn who writes to whom.
            assertEquals(FederatedEnvelope.HIDDEN_ROUTING_ID, mailboxes.delivered?.senderRoutingId)
            assertEquals(FederatedEnvelope.HIDDEN_ROUTING_ID, mailboxes.delivered?.recipientDeviceRoutingId)
            assertEquals(null, client.federatedEnvelope)
        }

    @Test
    fun theContactsRouteIsFoundByTheContactEvenWhenTheRoutingTableDoesNotKnowTheAddress() =
        runTest {
            val client = RecordingWebSocketTransportClient()
            val mailboxes = RecordingMailboxGateway()
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRoutingIdProvider = SuccessfulLocalRoutingIdProvider(),
                    localBootstrapRoutingIdProvider = SuccessfulLocalBootstrapRoutingIdProvider(),
                    transportConfig = TransportConfig(),
                    mailboxRouteRepository = FakeMailboxRouteRepository(ROUTE, knowsAddress = false),
                    mailboxGateway = mailboxes,
                    sendingPolicy = policy(sendsDirectly = true)
                )

            assertTrue(sender.sendWithAcceptance("scrouting1_contact", "encoded-payload", recipientContactId = "contact").isSuccess)

            assertEquals(ROUTE, mailboxes.delivered?.mailboxRoute)
        }

    @Test
    fun anUnreachableMailboxLeavesItToTheConnectedNodeWhenSendingDirectlyIsAllowed() =
        runTest {
            val client = RecordingWebSocketTransportClient()
            val mailboxes = RecordingMailboxGateway(fails = true)

            assertTrue(mailboxSender(client, mailboxes, sendsDirectly = true).send("recipient-routing-id", "encoded-payload").isSuccess)

            assertEquals(ROUTE, client.federatedEnvelope?.mailboxRoute)
            assertEquals("recipient-routing-id", client.federatedEnvelope?.recipientDeviceRoutingId)
        }

    @Test
    fun anUnreachableMailboxFailsTheSendWhenSendingDirectlyIsOff() =
        runTest {
            val client = RecordingWebSocketTransportClient()
            val mailboxes = RecordingMailboxGateway(fails = true)

            assertTrue(mailboxSender(client, mailboxes, sendsDirectly = false).send("recipient-routing-id", "encoded-payload").isFailure)

            assertEquals(null, client.federatedEnvelope)
        }

    private fun mailboxSender(
        client: RecordingWebSocketTransportClient,
        mailboxes: MailboxGateway,
        sendsDirectly: Boolean
    ) = WebSocketOutgoingWireSender(
        webSocketTransportClient = client,
        localRoutingIdProvider = SuccessfulLocalRoutingIdProvider(),
        localBootstrapRoutingIdProvider = SuccessfulLocalBootstrapRoutingIdProvider(),
        transportConfig = TransportConfig(),
        mailboxRouteRepository = FakeMailboxRouteRepository(ROUTE),
        mailboxGateway = mailboxes,
        sendingPolicy = policy(sendsDirectly)
    )

    /** No circuit is ever up here, so only the user's choice decides. */
    private fun policy(sendsDirectly: Boolean) =
        DirectSendingPolicy(
            circuits =
                CircuitRunner(
                    httpClient = HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) }),
                    cipher = ReadableCircuitCipher(),
                    selector = RelaySelector(EmptyCache, createGatewayJson(), preferredNodeStore = null, connectedNodeId = { null }),
                    hopCountStore = FixedHopCount,
                    circuitCountStore = OneCircuit
                ),
            fallback = FixedFallback(sendsDirectly)
        )

    @Test
    fun withoutAMailboxRouteThePacketGoesIntoTheRecipientsInvitationMailbox() =
        runTest {
            val client = RecordingWebSocketTransportClient()
            val world = InvitationTestWorld()
            val bob = world.device("bob")
            world.publish(bob)

            val acceptance = invitationSender(client, world).sendWithAcceptance(bob.routingId, "encoded-payload").getOrThrow()

            val envelope = world.mailboxes.deposited.single()
            assertEquals(bob.route, envelope.mailboxRoute)
            assertEquals(envelope.expiresAtEpochMilliseconds, acceptance.expiresAtEpochMilliseconds)
            assertEquals(null, client.envelope)
        }

    @Test
    fun aFirstInvitationIsSentAsTheOwnPhoneAliasOnceItIsBound() =
        runTest {
            val client = RecordingWebSocketTransportClient()
            val world = InvitationTestWorld()
            val bob = world.device("bob")
            world.publish(bob)

            assertTrue(invitationSender(client, world).send(bob.alias, "encoded-payload").isSuccess)

            assertEquals("local-bootstrap-routing-id", client.awaitedRoutingAlias)
            val letter = world.letters(bob).open(world.mailboxes.deposited.single()).exceptionOrNull()
            // The test sender's alias is not bound to its key in this world, so Bob refuses it: the alias was sent.
            assertTrue(letter is InvalidInvitationLetterException)
        }

    @Test
    fun aRecipientWithoutInvitationMailboxFailsTheSend() =
        runTest {
            val world = InvitationTestWorld()

            val result = invitationSender(RecordingWebSocketTransportClient(), world).send("scrouting1_nobody", "encoded-payload")

            assertTrue(result.exceptionOrNull() is InvitationRouteMissingException)
            assertTrue(world.mailboxes.deposited.isEmpty())
        }

    @Test
    fun localRoutingIdFailurePreventsTransportCall() =
        runTest {
            val expectedError = IllegalStateException("local routing ID unavailable")
            val world = InvitationTestWorld()
            world.publish(world.device("bob"))
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = RecordingWebSocketTransportClient(),
                    localRoutingIdProvider =
                        object : LocalRoutingIdProvider {
                            override suspend fun getLocalRoutingId(): Result<String> = Result.failure(expectedError)
                        },
                    localBootstrapRoutingIdProvider = SuccessfulLocalBootstrapRoutingIdProvider(),
                    transportConfig = TransportConfig(),
                    invitationLetters = world.letters(world.device("alice"))
                )

            val result = sender.send("scrouting1_bob", "encoded-payload")

            assertSame(expectedError, result.exceptionOrNull())
            assertTrue(world.mailboxes.deposited.isEmpty())
        }

    private fun invitationSender(
        client: RecordingWebSocketTransportClient,
        world: InvitationTestWorld
    ) = WebSocketOutgoingWireSender(
        webSocketTransportClient = client,
        localRoutingIdProvider = SuccessfulLocalRoutingIdProvider(),
        localBootstrapRoutingIdProvider = SuccessfulLocalBootstrapRoutingIdProvider(),
        transportConfig = TransportConfig(acknowledgementTimeoutMilliseconds = 2_500L),
        invitationLetters = world.letters(world.device("alice"))
    )

    private object EmptyCache : NodeDirectoryCache {
        override suspend fun read(): CachedNodeDirectory? = null

        override suspend fun write(directory: CachedNodeDirectory) = Unit
    }

    private object OneCircuit : CircuitCountStore {
        override fun observe(): Flow<Int> = flowOf(1)

        override suspend fun set(circuitCount: Int) = Unit
    }

    private object FixedHopCount : CircuitHopCountStore {
        override fun observe(): Flow<Int> = flowOf(2)

        override suspend fun set(hopCount: Int) = Unit
    }

    private class RecordingMailboxGateway(
        private val fails: Boolean = false
    ) : MailboxGateway {
        var delivered: FederatedEnvelope? = null

        override suspend fun deliver(envelope: FederatedEnvelope): Result<Unit> =
            if (fails) {
                Result.failure(IllegalStateException("Mailbox delivery failed with 502"))
            } else {
                delivered = envelope
                Result.success(Unit)
            }

        override suspend fun create(
            contactId: String,
            nodeId: String,
            routeEndpoint: String,
            accessEndpoint: String,
            sequence: Long,
            expiresAtEpochMilliseconds: Long
        ): Result<LocalMailboxCredential> = error("Not used")

        override suspend fun createInvitation(
            nodeId: String,
            routeEndpoint: String,
            accessEndpoint: String,
            expiresAtEpochMilliseconds: Long
        ): Result<LocalMailboxCredential> = error("Not used")

        override suspend fun depositInvitation(envelope: FederatedEnvelope): Result<Unit> = error("Not used")

        override suspend fun pending(credential: LocalMailboxCredential): Result<List<FederatedEnvelope>> = error("Not used")

        override suspend fun acknowledge(
            credential: LocalMailboxCredential,
            envelopeId: String
        ): Result<Unit> = error("Not used")

        override suspend fun revoke(credential: LocalMailboxCredential): Result<Unit> = error("Not used")

        override suspend fun ready(
            accessEndpoint: String,
            credentials: List<LocalMailboxCredential>
        ): Result<Set<String>> = error("Not used")
    }

    private class FixedFallback(
        private val sendsDirectly: Boolean
    ) : CircuitFallbackStore {
        override fun observeSendsDirectly(): Flow<Boolean> = flowOf(sendsDirectly)

        override suspend fun setSendsDirectly(sendsDirectly: Boolean) = Unit
    }

    private class SuccessfulLocalRoutingIdProvider : LocalRoutingIdProvider {
        override suspend fun getLocalRoutingId(): Result<String> = Result.success("local-routing-id")
    }

    private class SuccessfulLocalBootstrapRoutingIdProvider : LocalBootstrapRoutingIdProvider {
        override suspend fun getLocalBootstrapRoutingId(): Result<String> =
            Result.success("local-bootstrap-routing-id")
    }

    private class RecordingWebSocketTransportClient(
        private val sendResult: Result<Unit> = Result.success(Unit)
    ) : WebSocketTransportClient {
        var envelope: TransportEnvelope? = null
        var federatedEnvelope: FederatedEnvelope? = null
        var timeoutMilliseconds: Long? = null
        var awaitedRoutingAlias: String? = null
        var aliasTimeoutMilliseconds: Long? = null

        override val connectionState: StateFlow<TransportConnectionState> =
            MutableStateFlow(TransportConnectionState.Connected("local-routing-id"))
        override val incomingEnvelopes: Flow<TransportEnvelope> = MutableSharedFlow()

        override fun connect(
            serverUrl: String,
            localRoutingId: String,
            expectedNodeId: String
        ) = Unit

        override suspend fun awaitRoutingAlias(
            routingAlias: String,
            timeoutMilliseconds: Long
        ): Result<Unit> {
            awaitedRoutingAlias = routingAlias
            aliasTimeoutMilliseconds = timeoutMilliseconds
            return Result.success(Unit)
        }

        override suspend fun sendEnvelopeAndAwaitAcceptance(
            envelope: TransportEnvelope,
            timeoutMilliseconds: Long
        ): Result<Unit> {
            this.envelope = envelope
            this.timeoutMilliseconds = timeoutMilliseconds

            return sendResult
        }

        override suspend fun sendFederatedEnvelopeAndAwaitAcceptance(
            envelope: FederatedEnvelope,
            timeoutMilliseconds: Long
        ): Result<Unit> {
            federatedEnvelope = envelope
            this.timeoutMilliseconds = timeoutMilliseconds
            return sendResult
        }

        override suspend fun acknowledgeIncomingEnvelope(envelopeId: String): Result<Unit> = Result.success(Unit)

        override suspend fun disconnect() = Unit
    }

    /** Like the app's table: with [knowsAddress] false, only the lookup by contact finds the route. */
    private class FakeMailboxRouteRepository(
        private val route: MailboxDeliveryRoute,
        private val knowsAddress: Boolean = true
    ) : MailboxRouteRepository {
        override suspend fun localForContact(contactId: String) = Result.success<LocalMailboxCredential?>(null)

        override suspend fun remoteForRecipientRoutingId(routingId: String) = Result.success(route.takeIf { knowsAddress })

        override suspend fun remoteForContact(contactId: String) = Result.success(route)

        override suspend fun allLocal() = Result.success(emptyList<LocalMailboxCredential>())

        override suspend fun saveLocal(credential: LocalMailboxCredential) = Result.success(Unit)

        override suspend fun saveRemote(
            contactId: String,
            route: MailboxDeliveryRoute
        ) = Result.success(Unit)

        override suspend fun markLocalRevocationPending(contactId: String) = Result.success(Unit)

        override suspend fun deleteLocal(contactId: String) = Result.success(Unit)

        override suspend fun deleteRemote(contactId: String) = Result.success(Unit)

        override suspend fun deleteAllRemote() = Result.success(Unit)

        override suspend fun allRetiring() = Result.success(emptyList<LocalMailboxCredential>())

        override suspend fun retireLocal(
            retiring: LocalMailboxCredential,
            replacement: LocalMailboxCredential
        ) = Result.success(Unit)

        override suspend fun saveRetiring(credential: LocalMailboxCredential) = Result.success(Unit)

        override suspend fun markRetiringRevocationPending(contactId: String) = Result.success(Unit)

        override suspend fun deleteRetiring(routeId: String) = Result.success(Unit)
    }

    private companion object {
        val ROUTE =
            MailboxDeliveryRoute(
                routeId = "route-1",
                nodeId = "node-b",
                nodeEndpoint = "https://node-b.example",
                mailboxId = "mailbox-1",
                sendCapability = "send-capability",
                sequence = 4L,
                expiresAtEpochMilliseconds = Long.MAX_VALUE,
                identitySignature = byteArrayOf(1, 2, 3)
            )
    }
}
