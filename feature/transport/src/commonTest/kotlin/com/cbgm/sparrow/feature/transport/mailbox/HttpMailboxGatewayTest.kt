package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.core.crypto.hash.DefaultCryptoHash
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
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

class HttpMailboxGatewayTest {
    @Test
    fun aDownNodeIsReportedAsUnavailable() =
        runTest {
            val result = gateway(HttpStatusCode.BadGateway).pending(credential)

            assertIs<MailboxNodeUnavailableException>(result.exceptionOrNull())
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
                HttpMailboxGateway(
                    HttpClient(engine) {
                        expectSuccess = true
                        install(ContentNegotiation) { json(createGatewayJson()) }
                    },
                    createGatewayJson(),
                    MailboxCreationWorkSolver(DefaultCryptoHash())
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
                HttpMailboxGateway(
                    HttpClient(engine) { install(ContentNegotiation) { json(createGatewayJson()) } },
                    createGatewayJson(),
                    MailboxCreationWorkSolver(DefaultCryptoHash()),
                    circuitSlot = { contactId -> if (contactId == "contact-box-a") 0 else 1 }
                )

            val ready = gateway.ready("https://node.test", listOf(credentialFor("box-a"), credentialFor("box-b"))).getOrThrow()

            assertEquals(setOf("box-a", "box-b"), ready)
            assertEquals(2, bodies.size)
            assertEquals(1, bodies.count { "box-a" in it && "box-b" !in it })
        }

    private fun credentialFor(mailboxId: String) =
        credential.copy(contactId = "contact-$mailboxId", deliveryRoute = credential.deliveryRoute.copy(mailboxId = mailboxId))

    private fun gateway(status: HttpStatusCode) =
        HttpMailboxGateway(
            HttpClient(MockEngine { respond(content = "", status = status) }) { expectSuccess = true },
            createGatewayJson(),
            MailboxCreationWorkSolver(DefaultCryptoHash())
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
