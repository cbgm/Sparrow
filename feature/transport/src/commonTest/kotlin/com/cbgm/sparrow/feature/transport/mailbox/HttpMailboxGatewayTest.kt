package com.cbgm.sparrow.feature.transport.mailbox

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
                    createGatewayJson()
                )

            val envelopes = gateway.pending(credential).getOrThrow()

            assertEquals(listOf("fine"), envelopes.map { it.envelopeId })
            assertEquals(listOf("broken"), deleted)
        }

    private fun gateway(status: HttpStatusCode) =
        HttpMailboxGateway(
            HttpClient(MockEngine { respond(content = "", status = status) }) { expectSuccess = true },
            createGatewayJson()
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
