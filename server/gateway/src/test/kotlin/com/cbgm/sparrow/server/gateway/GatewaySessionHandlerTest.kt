package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.EnvelopeAcceptanceState
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.FederationAcknowledgement
import com.cbgm.sparrow.server.protocol.GatewayClientMessage
import com.cbgm.sparrow.server.protocol.GatewayServerMessage
import com.cbgm.sparrow.server.protocol.TransportEnvelope
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.GatewaySessionBinding
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.ProtocolSignatures
import com.cbgm.sparrow.server.security.Signatures
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.server.websocket.WebSockets as ServerWebSockets

class GatewaySessionHandlerTest {
    private val pendingCalls = AtomicInteger()
    private val acknowledgeCalls = AtomicInteger()
    private val client = NodeIdentity.generate()
    private val routingId = ClientRoutingIds.fromSigningPublicKey(client.encodedPublicKey)
    private val connections = ConnectionRegistry()

    @Test
    fun sessionStartsWithSingleUseChallengeForThisNode() =
        gatewayTest {
            val first = connect { challenge() }
            val second = connect { challenge() }

            assertEquals(NODE_ID, first.nodeId)
            assertEquals(32, first.nonce.size)
            assertTrue(!first.nonce.contentEquals(second.nonce))
        }

    @Test
    fun registerWithoutProofIsRejectedAndReceivesNoPendingEnvelopes() =
        gatewayTest {
            connect {
                challenge()
                sendText("""{"type":"register","routingId":"$routingId","connectionId":"$CONNECTION_ID"}""")

                assertInvalidSession("Register with a session proof first")
            }

            assertEquals(0, pendingCalls.get())
            assertEquals(0, connections.count())
        }

    @Test
    fun keyOfAnotherIdentityCannotRegisterThisRoutingId() =
        gatewayTest {
            val attacker = NodeIdentity.generate()
            connect {
                val challenge = challenge()
                send(
                    register(
                        nonce = challenge.nonce,
                        signer = attacker,
                        signingPublicKey = attacker.encodedPublicKey
                    )
                )

                assertInvalidSession("Session proof is invalid: ROUTING_IDENTITY")
            }

            assertEquals(0, pendingCalls.get())
        }

    @Test
    fun bootstrapRoutingIdCannotRegister() =
        gatewayTest {
            connect {
                val challenge = challenge()
                send(register(nonce = challenge.nonce, routingId = "scphone1_test"))

                assertInvalidSession("Session proof is invalid: BOOTSTRAP_ROUTING_ID")
            }
        }

    @Test
    fun signatureOverAnotherNonceIsRejected() =
        gatewayTest {
            connect {
                challenge()
                send(register(nonce = ByteArray(32) { 7 }))

                assertInvalidSession("Session proof is invalid: SIGNATURE")
            }

            assertEquals(0, pendingCalls.get())
        }

    @Test
    fun proofFromAnEarlierSessionCannotBeReplayed() =
        gatewayTest {
            val earlierProof =
                connect {
                    val challenge = challenge()
                    register(nonce = challenge.nonce).also { proof -> send(proof) }
                        .also { assertIs<GatewayServerMessage.Registered>(receiveMessage()) }
                }

            connect {
                challenge()
                send(earlierProof)

                assertInvalidSession("Session proof is invalid: SIGNATURE")
            }
        }

    @Test
    fun signatureForAnotherNodeIsRejected() =
        gatewayTest {
            connect {
                val challenge = challenge()
                send(register(nonce = challenge.nonce, nodeId = "node-b"))

                assertInvalidSession("Session proof is invalid: SIGNATURE")
            }

            assertEquals(0, pendingCalls.get())
        }

    @Test
    fun validProofActivatesSessionAndDeliversPendingEnvelopes() =
        gatewayTest {
            connect {
                val challenge = challenge()
                send(register(nonce = challenge.nonce))

                assertEquals(routingId, assertIs<GatewayServerMessage.Registered>(receiveMessage()).routingId)
                val incoming = assertIs<GatewayServerMessage.IncomingEnvelope>(receiveMessage())
                assertEquals("pending-1", incoming.envelope.envelopeId)

                send(register(nonce = challenge.nonce))
                assertEquals("ALREADY_REGISTERED", assertIs<GatewayServerMessage.Error>(receiveMessage()).code)
            }
        }

    @Test
    fun anyOtherFrameBeforeSignInClosesTheSocket() =
        gatewayTest {
            connect {
                challenge()
                send(GatewayClientMessage.AcknowledgeEnvelope(envelopeId = "pending-1"))

                assertInvalidSession("Register with a session proof first")
            }
            connect {
                challenge()
                send(GatewayClientMessage.SendEnvelope(transportEnvelope(senderId = routingId)))

                assertInvalidSession("Register with a session proof first")
            }

            assertEquals(0, acknowledgeCalls.get())
            assertEquals(0, pendingCalls.get())
        }

    @Test
    fun socketWithoutSignInIsClosedAtTheDeadline() =
        gatewayTest(signInTimeoutMilliseconds = 200L) {
            connect {
                challenge()

                assertInvalidSession("Sign-in timed out")
            }
        }

    @Test
    fun signedInSessionOutlivesTheDeadline() =
        gatewayTest(signInTimeoutMilliseconds = 200L) {
            connect {
                send(register(nonce = challenge().nonce))
                assertIs<GatewayServerMessage.Registered>(receiveMessage())
                assertIs<GatewayServerMessage.IncomingEnvelope>(receiveMessage())

                delay(500L.milliseconds)
                send(GatewayClientMessage.SendEnvelope(transportEnvelope(senderId = "scrouting1_other")))
                assertEquals("SENDER_MISMATCH", assertIs<GatewayServerMessage.Error>(receiveMessage()).code)
            }
        }

    @Test
    fun envelopesWithAnotherSenderAreRejected() =
        gatewayTest {
            connect {
                val challenge = challenge()
                send(register(nonce = challenge.nonce))
                assertIs<GatewayServerMessage.Registered>(receiveMessage())
                assertIs<GatewayServerMessage.IncomingEnvelope>(receiveMessage())

                send(GatewayClientMessage.SendEnvelope(transportEnvelope(senderId = "scrouting1_other")))
                assertEquals("SENDER_MISMATCH", assertIs<GatewayServerMessage.Error>(receiveMessage()).code)

                send(GatewayClientMessage.SendFederatedEnvelope(federatedEnvelope(sender = "scrouting1_other")))
                assertEquals("SENDER_MISMATCH", assertIs<GatewayServerMessage.Error>(receiveMessage()).code)
            }
        }

    private fun gatewayTest(
        signInTimeoutMilliseconds: Long = 10_000L,
        block: suspend ApplicationTestBuilder.() -> Unit
    ) =
        testApplication {
            val handler = gatewayHandler(signInTimeoutMilliseconds)
            install(ServerWebSockets)
            routing {
                webSocket("/v1/gateway") { handler.handle(this) }
            }
            try {
                block()
            } finally {
                handler.close()
            }
        }

    private suspend fun <T> ApplicationTestBuilder.connect(block: suspend DefaultClientWebSocketSession.() -> T): T {
        var result: Result<T>? = null
        createClient { install(ClientWebSockets) }.webSocket("/v1/gateway") {
            result = runCatching { withRealTimeout { block() } }
        }
        return checkNotNull(result).getOrThrow()
    }

    private fun gatewayHandler(signInTimeoutMilliseconds: Long): GatewayWebSocketHandler =
        GatewayWebSocketHandler(
            nodeId = NODE_ID,
            connections = connections,
            federation =
                object : FederationClient {
                    override suspend fun route(envelope: FederatedEnvelope) =
                        FederationAcknowledgement(
                            envelopeId = envelope.envelopeId,
                            state = EnvelopeAcceptanceState.STORED_AT_DESTINATION
                        )
                },
            presence =
                object : PresenceClient {
                    override suspend fun register(registration: ClientRouteRegistration) =
                        PresenceRegistrationResult.ACCEPTED

                    override suspend fun remove(
                        routingId: String,
                        connectionId: String
                    ) = Unit
                },
            legacyPush =
                object : LegacyPushClient {
                    override suspend fun store(envelope: TransportEnvelope): Boolean = true

                    override suspend fun pending(recipientId: String): List<TransportEnvelope> {
                        pendingCalls.incrementAndGet()
                        return listOf(transportEnvelope(envelopeId = "pending-1", senderId = "scrouting1_peer"))
                            .filter { envelope -> recipientId == envelope.recipientId }
                    }

                    override suspend fun acknowledge(
                        recipientId: String,
                        envelopeId: String
                    ) {
                        acknowledgeCalls.incrementAndGet()
                    }
                },
            routeLifetimeMilliseconds = 90_000L,
            signInTimeoutMilliseconds = signInTimeoutMilliseconds,
            blobUploadTicketIssuer =
                GatewayBlobUploadTicketIssuer(
                    nodeId = NODE_ID,
                    permitStore = BlobUploadPermitStore(),
                    maximumBlobBytes = 1_024L,
                    maximumRetentionMilliseconds = 60_000L,
                    ticketLifetimeMilliseconds = 30_000L
                )
        )

    private fun register(
        nonce: ByteArray,
        routingId: String = this.routingId,
        nodeId: String = NODE_ID,
        signer: NodeIdentity = client,
        signingPublicKey: ByteArray = client.encodedPublicKey
    ): GatewayClientMessage.Register =
        GatewayClientMessage.Register(
            routingId = routingId,
            connectionId = CONNECTION_ID,
            clientSigningPublicKey = signingPublicKey,
            clientSignature =
                Signatures.sign(
                    ProtocolSignatures.gatewaySessionContent(
                        GatewaySessionBinding(
                            routingId = routingId,
                            connectionId = CONNECTION_ID,
                            nodeId = nodeId,
                            nonce = nonce
                        )
                    ),
                    signer.privateKey
                )
        )

    private fun transportEnvelope(
        envelopeId: String = "envelope-1",
        senderId: String
    ): TransportEnvelope =
        TransportEnvelope(
            envelopeId = envelopeId,
            senderId = senderId,
            recipientId = routingId,
            payload = "ciphertext",
            createdAtEpochMilliseconds = 1L
        )

    private fun federatedEnvelope(sender: String): FederatedEnvelope =
        FederatedEnvelope(
            envelopeId = "envelope-2",
            senderRoutingId = sender,
            recipientDeviceRoutingId = "scrouting1_peer",
            mailboxRoute = null,
            encryptedPayload = "ciphertext",
            createdAtEpochMilliseconds = 1L,
            expiresAtEpochMilliseconds = Long.MAX_VALUE
        )

    private suspend fun DefaultClientWebSocketSession.challenge(): GatewayServerMessage.Challenge =
        assertIs<GatewayServerMessage.Challenge>(receiveMessage())

    private suspend fun DefaultClientWebSocketSession.assertInvalidSession(message: String) {
        val error = assertIs<GatewayServerMessage.Error>(receiveMessage())
        assertEquals("INVALID_SESSION", error.code)
        assertEquals(message, error.message)
        assertTrue(incoming.receiveCatching().isClosed, "The gateway must close a rejected session")
    }

    private suspend fun DefaultClientWebSocketSession.receiveMessage(): GatewayServerMessage =
        serverJson.decodeFromString<GatewayServerMessage>((incoming.receive() as Frame.Text).readText())

    private suspend fun DefaultClientWebSocketSession.send(message: GatewayClientMessage) {
        sendText(serverJson.encodeToString<GatewayClientMessage>(message))
    }

    private suspend fun DefaultClientWebSocketSession.sendText(text: String) {
        send(Frame.Text(text))
    }

    private companion object {
        const val NODE_ID = "node-a"
        const val CONNECTION_ID = "connection-a"
    }
}
