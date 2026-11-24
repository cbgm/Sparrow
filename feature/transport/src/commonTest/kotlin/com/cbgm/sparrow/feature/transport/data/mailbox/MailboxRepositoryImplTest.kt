package com.cbgm.sparrow.feature.transport.data.mailbox

import com.cbgm.sparrow.core.crypto.hash.Sha256CryptoHash
import com.cbgm.sparrow.feature.transport.domain.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.domain.model.MailboxGoneException
import com.cbgm.sparrow.feature.transport.domain.model.MailboxNodeUnavailableException
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class MailboxRepositoryImplTest {
    @Test
    fun aDownNodeIsReportedAsUnavailable() =
        runTest {
            val result = gateway(HttpStatusCode.BadGateway).pending(credential)

            assertIs<MailboxNodeUnavailableException>(result.exceptionOrNull())
        }

    @Test
    fun aDownNodeAnsweredByItsProxyIsReportedAsUnavailable() =
        runTest {
            val result = mailboxRepository(HttpStatusCode.BadGateway).pending(credential)

            assertIs<MailboxNodeUnavailableException>(result.exceptionOrNull())
        }

    @Test
    fun deliveringToADownNodeIsReportedAsUnavailable() =
        runTest {
            val envelope =
                FederatedEnvelope(
                    envelopeId = "envelope",
                    senderRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
                    recipientDeviceRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
                    mailboxRoute = credential.deliveryRoute,
                    encryptedPayload = "x",
                    createdAtEpochMilliseconds = 1L,
                    expiresAtEpochMilliseconds = 2L
                )

            val result = mailboxRepository(HttpStatusCode.BadGateway).deliver(envelope)

            assertIs<MailboxNodeUnavailableException>(result.exceptionOrNull())
        }

    @Test
    fun aMailboxTheNodeNoLongerKnowsIsGone() =
        runTest {
            val result = mailboxRepository(HttpStatusCode.NotFound).pending(credential)

            assertIs<MailboxGoneException>(result.exceptionOrNull())
        }

    @Test
    fun aRefusedRequestIsNotTreatedAsADownNode() =
        runTest {
            val error = gateway(HttpStatusCode.Unauthorized).pending(credential).exceptionOrNull()

            assertFalse(error is MailboxNodeUnavailableException)
        }

    @Test
    fun anUnreadableEnvelopeIsAcknowledgedAndTheRestArrive() =
        runTest {
            val deleted = mutableListOf<String>()
            val engine =
                MockEngine { request ->
                    if (request.method == HttpMethod.Delete) {
                        deleted += request.url.encodedPath.substringAfterLast('/')
                        respond(content = "", status = HttpStatusCode.NoContent)
                    } else {
                        respond(
                            content =
                                """{"envelopes":[{"envelopeId":"broken"},""" +
                                    """{"envelopeId":"fine","senderRoutingId":"a","recipientDeviceRoutingId":"b",""" +
                                    """"encryptedPayload":"x","createdAtEpochMilliseconds":1,"expiresAtEpochMilliseconds":2}]}""",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    }
                }
            val gateway =
                MailboxRepositoryImpl(
                    HttpClient(engine) {
                        expectSuccess = true
                        install(ContentNegotiation) { json(createGatewayJson()) }
                    },
                    createGatewayJson(),
                    MailboxCreationWorkSolver(Sha256CryptoHash())
                )

            val envelopes = gateway.pending(credential).getOrThrow()

            assertEquals(listOf("fine"), envelopes.map { it.envelopeId })
            assertEquals(listOf("broken"), deleted)
        }

    @Test
    fun eachCircuitWaitsOnlyForItsOwnMailboxes() =
        runTest {
            val bodies = mutableListOf<String>()
            val engine =
                MockEngine { request ->
                    val body = (request.body as io.ktor.http.content.TextContent).text
                    bodies += body
                    respond(
                        content = """{"ready":["${if ("box-a" in body) "box-a" else "box-b"}"]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            val gateway =
                MailboxRepositoryImpl(
                    HttpClient(engine) { install(ContentNegotiation) { json(createGatewayJson()) } },
                    createGatewayJson(),
                    MailboxCreationWorkSolver(Sha256CryptoHash()),
                    circuitSlot = { contactId -> if (contactId == "contact-box-a") 0 else 1 }
                )

            val ready = gateway.ready("https://node.test", listOf(credentialFor("box-a"), credentialFor("box-b"))).getOrThrow()

            assertEquals(setOf("box-a", "box-b"), ready)
            assertEquals(2, bodies.size)
            assertEquals(1, bodies.count { "box-a" in it && "box-b" !in it })
        }

    private fun credentialFor(mailboxId: String) =
        credential.copy(contactId = "contact-$mailboxId", deliveryRoute = credential.deliveryRoute.copy(mailboxId = mailboxId))

    /** Like the app's mailbox client: status codes are checked by the gateway, not thrown by the client. */
    private fun mailboxRepository(status: HttpStatusCode) =
        MailboxRepositoryImpl(
            HttpClient(MockEngine { respond(content = "", status = status) }) {
                install(ContentNegotiation) { json(createGatewayJson()) }
            },
            createGatewayJson(),
            MailboxCreationWorkSolver(Sha256CryptoHash())
        )

    private fun gateway(status: HttpStatusCode) =
        MailboxRepositoryImpl(
            HttpClient(MockEngine { respond(content = "", status = status) }) { expectSuccess = true },
            createGatewayJson(),
            MailboxCreationWorkSolver(Sha256CryptoHash())
        )

    private val credential =
        LocalMailboxCredential(
            contactId = "contact",
            deliveryRoute =
                MailboxDeliveryRoute(
                    routeId = "route",
                    nodeId = "node",
                    nodeEndpoint = "https://node.example.org",
                    mailboxId = "mailbox",
                    sendCapability = "send",
                    sequence = 0L,
                    expiresAtEpochMilliseconds = Long.MAX_VALUE,
                    identitySignature = byteArrayOf()
                ),
            accessEndpoint = "https://node.example.org",
            retrievalCapability = "retrieve"
        )
}
