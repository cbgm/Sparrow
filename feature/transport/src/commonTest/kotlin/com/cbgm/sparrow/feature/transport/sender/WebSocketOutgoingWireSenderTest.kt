package com.cbgm.sparrow.feature.transport.sender

import com.cbgm.sparrow.feature.transport.circuit.CircuitCountStore
import com.cbgm.sparrow.feature.transport.circuit.CircuitFallbackStore
import com.cbgm.sparrow.feature.transport.circuit.CircuitHopCountStore
import com.cbgm.sparrow.feature.transport.circuit.CircuitRunner
import com.cbgm.sparrow.feature.transport.circuit.DirectSendingPolicy
import com.cbgm.sparrow.feature.transport.circuit.ReadableCircuitCipher
import com.cbgm.sparrow.feature.transport.circuit.isWaitingForRoute
import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionState
import com.cbgm.sparrow.feature.transport.discovery.CachedNodeDirectory
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryCache
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayIndicatorEvent
import com.cbgm.sparrow.feature.transport.gateway.model.TransportEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.mailbox.MailboxWakeTicket
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
    fun withoutAMailboxRouteNothingGoesOutWhileSendingDirectlyIsOffAndNoCircuitProtects() =
        runTest {
            val client = RecordingWebSocketTransportClient()
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRoutingIdProvider = SuccessfulLocalRoutingIdProvider(),
                    localBootstrapRoutingIdProvider = SuccessfulLocalBootstrapRoutingIdProvider(),
                    transportConfig = TransportConfig(),
                    sendingPolicy = policy(sendsDirectly = false)
                )

            val result = sender.send("recipient-routing-id", "encoded-payload")

            assertTrue(result.exceptionOrNull()?.isWaitingForRoute() == true)
            assertEquals(null, client.envelope)
        }

    @Test
    fun withoutAMailboxRouteTheConnectedNodeDeliversWhenSendingDirectlyIsAllowed() =
        runTest {
            val client = RecordingWebSocketTransportClient()
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRoutingIdProvider = SuccessfulLocalRoutingIdProvider(),
                    localBootstrapRoutingIdProvider = SuccessfulLocalBootstrapRoutingIdProvider(),
                    transportConfig = TransportConfig(),
                    sendingPolicy = policy(sendsDirectly = true)
                )

            assertTrue(sender.send("recipient-routing-id", "encoded-payload").isSuccess)
            assertEquals("recipient-routing-id", client.envelope?.recipientId)
        }

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
            expiresAtEpochMilliseconds: Long,
            wakeTicket: MailboxWakeTicket
        ): Result<LocalMailboxCredential> = error("Not used")

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

    @Test
    fun sendBuildsEnvelopeAndWaitsForGatewayAcceptance() =
        runTest {
            val client = RecordingWebSocketTransportClient()
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRoutingIdProvider = SuccessfulLocalRoutingIdProvider(),
                    localBootstrapRoutingIdProvider = SuccessfulLocalBootstrapRoutingIdProvider(),
                    transportConfig =
                        TransportConfig(
                            acknowledgementTimeoutMilliseconds = 2_500L
                        )
                )

            val result =
                sender.send(
                    recipientAddress = "recipient-routing-id",
                    encodedTransportPayload = "encoded-payload"
                )

            assertTrue(result.isSuccess)
            val envelope = requireNotNull(client.envelope)
            assertTrue(envelope.envelopeId.isNotBlank())
            assertEquals("local-routing-id", envelope.senderId)
            assertEquals("recipient-routing-id", envelope.recipientId)
            assertEquals("encoded-payload", envelope.payload)
            assertTrue(envelope.createdAtEpochMilliseconds > 0L)
            assertEquals(2_500L, client.timeoutMilliseconds)
        }

    @Test
    fun bootstrapSendWaitsUntilLocalBootstrapAliasIsRegistered() =
        runTest {
            val client = RecordingWebSocketTransportClient()
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRoutingIdProvider = SuccessfulLocalRoutingIdProvider(),
                    localBootstrapRoutingIdProvider = SuccessfulLocalBootstrapRoutingIdProvider(),
                    transportConfig =
                        TransportConfig(
                            acknowledgementTimeoutMilliseconds = 2_500L
                        )
                )

            val result =
                sender.send(
                    recipientAddress = "scphone1_recipient",
                    encodedTransportPayload = "encoded-payload"
                )

            assertTrue(result.isSuccess)
            assertEquals("local-bootstrap-routing-id", client.awaitedRoutingAlias)
            assertEquals(2_500L, client.aliasTimeoutMilliseconds)
            val envelope = requireNotNull(client.envelope)
            assertEquals("local-bootstrap-routing-id", envelope.senderId)
            assertEquals("scphone1_recipient", envelope.recipientId)
        }

    @Test
    fun gatewayAcceptanceFailureIsPropagated() =
        runTest {
            val expectedError = IllegalStateException("gateway rejected envelope")
            val client =
                RecordingWebSocketTransportClient(
                    sendResult = Result.failure(expectedError)
                )
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRoutingIdProvider = SuccessfulLocalRoutingIdProvider(),
                    localBootstrapRoutingIdProvider = SuccessfulLocalBootstrapRoutingIdProvider(),
                    transportConfig =
                        TransportConfig()
                )

            val result =
                sender.send(
                    recipientAddress = "recipient-routing-id",
                    encodedTransportPayload = "encoded-payload"
                )

            assertTrue(result.isFailure)
            assertSame(expectedError, result.exceptionOrNull())
        }

    @Test
    fun localRoutingIdFailurePreventsTransportCall() =
        runTest {
            val expectedError = IllegalStateException("local routing ID unavailable")
            val client = RecordingWebSocketTransportClient()
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRoutingIdProvider =
                        object : LocalRoutingIdProvider {
                            override suspend fun getLocalRoutingId(): Result<String> = Result.failure(expectedError)
                        },
                    localBootstrapRoutingIdProvider = SuccessfulLocalBootstrapRoutingIdProvider(),
                    transportConfig =
                        TransportConfig()
                )

            val result =
                sender.send(
                    recipientAddress = "recipient-routing-id",
                    encodedTransportPayload = "encoded-payload"
                )

            assertTrue(result.isFailure)
            assertSame(expectedError, result.exceptionOrNull())
            assertEquals(null, client.envelope)
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
        override val incomingIndicatorEvents: Flow<GatewayIndicatorEvent> = MutableSharedFlow()

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

        override suspend fun sendIndicatorState(
            recipientId: String,
            indicatorType: String
        ): Result<Unit> = Result.success(Unit)

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
