package com.cbgm.securechat.feature.transport.sender

import com.cbgm.securechat.core.protocol.mailbox.LocalMailboxCredential
import com.cbgm.securechat.core.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.securechat.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import com.cbgm.securechat.feature.transport.relay.identity.LocalBootstrapRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.identity.LocalRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.model.FederatedEnvelope
import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope
import com.cbgm.securechat.feature.transport.relay.model.RelayTypingEvent
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
                    localRelayIdProvider = SuccessfulLocalRelayIdProvider(),
                    localBootstrapRelayIdProvider = SuccessfulLocalBootstrapRelayIdProvider(),
                    relayTransportConfig =
                        RelayTransportConfig(),
                    mailboxRouteRepository = FakeMailboxRouteRepository(route)
                )

            assertTrue(sender.send("recipient-relay-id", "encoded-payload").isSuccess)

            val envelope = requireNotNull(client.federatedEnvelope)
            assertEquals(route, envelope.mailboxRoute)
            assertEquals("local-relay-id", envelope.senderRoutingId)
            assertEquals("recipient-relay-id", envelope.recipientDeviceRoutingId)
            assertEquals("encoded-payload", envelope.encryptedPayload)
            assertEquals(null, client.envelope)
        }

    @Test
    fun sendBuildsEnvelopeAndWaitsForRelayAcceptance() =
        runTest {
            val client = RecordingWebSocketTransportClient()
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRelayIdProvider = SuccessfulLocalRelayIdProvider(),
                    localBootstrapRelayIdProvider = SuccessfulLocalBootstrapRelayIdProvider(),
                    relayTransportConfig =
                        RelayTransportConfig(
                            acknowledgementTimeoutMilliseconds = 2_500L
                        )
                )

            val result =
                sender.send(
                    recipientAddress = "recipient-relay-id",
                    encodedTransportPayload = "encoded-payload"
                )

            assertTrue(result.isSuccess)
            val envelope = requireNotNull(client.envelope)
            assertTrue(envelope.envelopeId.isNotBlank())
            assertEquals("local-relay-id", envelope.senderId)
            assertEquals("recipient-relay-id", envelope.recipientId)
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
                    localRelayIdProvider = SuccessfulLocalRelayIdProvider(),
                    localBootstrapRelayIdProvider = SuccessfulLocalBootstrapRelayIdProvider(),
                    relayTransportConfig =
                        RelayTransportConfig(
                            acknowledgementTimeoutMilliseconds = 2_500L
                        )
                )

            val result =
                sender.send(
                    recipientAddress = "scphone1_recipient",
                    encodedTransportPayload = "encoded-payload"
                )

            assertTrue(result.isSuccess)
            assertEquals("local-bootstrap-relay-id", client.awaitedRoutingAlias)
            assertEquals(2_500L, client.aliasTimeoutMilliseconds)
            val envelope = requireNotNull(client.envelope)
            assertEquals("local-bootstrap-relay-id", envelope.senderId)
            assertEquals("scphone1_recipient", envelope.recipientId)
        }

    @Test
    fun relayAcceptanceFailureIsPropagated() =
        runTest {
            val expectedError = IllegalStateException("relay rejected envelope")
            val client =
                RecordingWebSocketTransportClient(
                    sendResult = Result.failure(expectedError)
                )
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRelayIdProvider = SuccessfulLocalRelayIdProvider(),
                    localBootstrapRelayIdProvider = SuccessfulLocalBootstrapRelayIdProvider(),
                    relayTransportConfig =
                        RelayTransportConfig()
                )

            val result =
                sender.send(
                    recipientAddress = "recipient-relay-id",
                    encodedTransportPayload = "encoded-payload"
                )

            assertTrue(result.isFailure)
            assertSame(expectedError, result.exceptionOrNull())
        }

    @Test
    fun localRelayIdFailurePreventsTransportCall() =
        runTest {
            val expectedError = IllegalStateException("local relay ID unavailable")
            val client = RecordingWebSocketTransportClient()
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRelayIdProvider =
                        object : LocalRelayIdProvider {
                            override suspend fun getLocalRelayId(): Result<String> = Result.failure(expectedError)
                        },
                    localBootstrapRelayIdProvider = SuccessfulLocalBootstrapRelayIdProvider(),
                    relayTransportConfig =
                        RelayTransportConfig()
                )

            val result =
                sender.send(
                    recipientAddress = "recipient-relay-id",
                    encodedTransportPayload = "encoded-payload"
                )

            assertTrue(result.isFailure)
            assertSame(expectedError, result.exceptionOrNull())
            assertEquals(null, client.envelope)
        }

    private class SuccessfulLocalRelayIdProvider : LocalRelayIdProvider {
        override suspend fun getLocalRelayId(): Result<String> = Result.success("local-relay-id")
    }

    private class SuccessfulLocalBootstrapRelayIdProvider : LocalBootstrapRelayIdProvider {
        override suspend fun getLocalBootstrapRelayId(): Result<String> =
            Result.success("local-bootstrap-relay-id")
    }

    private class RecordingWebSocketTransportClient(
        private val sendResult: Result<Unit> = Result.success(Unit)
    ) : WebSocketTransportClient {
        var envelope: RelayEnvelope? = null
        var federatedEnvelope: FederatedEnvelope? = null
        var timeoutMilliseconds: Long? = null
        var awaitedRoutingAlias: String? = null
        var aliasTimeoutMilliseconds: Long? = null

        override val connectionState: StateFlow<TransportConnectionState> =
            MutableStateFlow(TransportConnectionState.Connected("local-relay-id"))
        override val incomingEnvelopes: Flow<RelayEnvelope> = MutableSharedFlow()
        override val incomingTypingEvents: Flow<RelayTypingEvent> = MutableSharedFlow()

        override fun connect(
            serverUrl: String,
            localRelayId: String
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
            envelope: RelayEnvelope,
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

        override suspend fun sendTypingState(
            recipientId: String,
            isTyping: Boolean
        ): Result<Unit> = Result.success(Unit)

        override suspend fun disconnect() = Unit
    }

    private class FakeMailboxRouteRepository(
        private val route: MailboxDeliveryRoute
    ) : MailboxRouteRepository {
        override suspend fun localForContact(contactId: String) = Result.success<LocalMailboxCredential?>(null)

        override suspend fun remoteForRecipientRoutingId(routingId: String) = Result.success(route)

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
    }
}
