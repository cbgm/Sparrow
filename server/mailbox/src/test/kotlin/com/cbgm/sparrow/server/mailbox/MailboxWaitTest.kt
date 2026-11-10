package com.cbgm.sparrow.server.mailbox

import com.cbgm.sparrow.server.protocol.CreateMailboxRequest
import com.cbgm.sparrow.server.protocol.CreateMailboxResponse
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.MailboxEnvelopeRequest
import com.cbgm.sparrow.server.protocol.MailboxEnvelopesResponse
import com.cbgm.sparrow.server.protocol.MailboxWaitEntry
import com.cbgm.sparrow.server.protocol.MailboxWaitRequest
import com.cbgm.sparrow.server.protocol.MailboxWaitResponse
import com.cbgm.sparrow.server.protocol.serverJson
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class MailboxWaitTest {
    @Test
    fun aWaitingOwnerLearnsAboutAnEnvelopeAsItArrives() =
        mailboxes {
            val mailbox = create()
            coroutineScope {
                val waiting = async { wait(MailboxWaitEntry(mailbox.deliveryRoute.mailboxId, mailbox.retrievalCapability)) }
                delay(300.milliseconds)
                store(mailbox, "envelope-1")

                assertEquals(listOf(mailbox.deliveryRoute.mailboxId), waiting.await())
            }
        }

    @Test
    fun aMailboxThatAlreadyHoldsEnvelopesIsReadyAtOnce() =
        mailboxes {
            val mailbox = create()
            store(mailbox, "envelope-1")

            val startedAt = System.currentTimeMillis()
            assertEquals(listOf(mailbox.deliveryRoute.mailboxId), wait(MailboxWaitEntry(mailbox.deliveryRoute.mailboxId, mailbox.retrievalCapability)))
            assertTrue(System.currentTimeMillis() - startedAt < 2_000)
        }

    @Test
    fun aWrongCapabilityNeverMakesAMailboxReady() =
        mailboxes {
            val mailbox = create()
            store(mailbox, "envelope-1")

            assertEquals(emptyList(), wait(MailboxWaitEntry(mailbox.deliveryRoute.mailboxId, "not-the-capability")))
        }

    @Test
    fun aFullMailboxAnswersInPagesThatFitThroughACircuit() =
        mailboxes {
            val mailbox = create()
            repeat(3) { index -> store(mailbox, "envelope-$index", payload = "x".repeat(600_000)) }

            val first =
                client.get("/v1/mailboxes/${mailbox.deliveryRoute.mailboxId}/envelopes") { bearerAuth(mailbox.retrievalCapability) }
            val page = serverJson.decodeFromString<MailboxEnvelopesResponse>(first.bodyAsText())

            assertEquals(listOf("envelope-0"), page.envelopes.map { it.envelopeId })
        }

    private fun mailboxes(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                mailboxModule(
                    config =
                        MailboxConfig(
                            databaseUrl = null,
                            databaseUser = "",
                            databasePassword = "",
                            databaseMaximumPoolSize = 1,
                            maximumEnvelopeBytes = 1_048_576,
                            maximumMailboxBytes = 100L * 1_048_576L,
                            creationWorkBits = 0
                        ),
                    store = MailboxStore()
                )
            }
            block()
        }

    private suspend fun ApplicationTestBuilder.create(): CreateMailboxResponse {
        val response =
            client.post("/v1/mailboxes") {
                contentType(ContentType.Application.Json)
                setBody(
                    serverJson.encodeToString(
                        CreateMailboxRequest(
                            nodeId = "node-a",
                            nodeEndpoint = "http://mailbox",
                            expiresAtEpochMilliseconds = System.currentTimeMillis() + 60_000L,
                            creationWork = solvedWork(0)
                        )
                    )
                )
            }
        assertEquals(HttpStatusCode.Created, response.status)
        return serverJson.decodeFromString(response.bodyAsText())
    }

    private suspend fun ApplicationTestBuilder.store(
        mailbox: CreateMailboxResponse,
        envelopeId: String,
        payload: String = "payload"
    ) {
        val now = System.currentTimeMillis()
        val envelope =
            FederatedEnvelope(
                envelopeId = envelopeId,
                senderRoutingId = "sender",
                recipientDeviceRoutingId = "recipient",
                mailboxRoute = mailbox.deliveryRoute,
                encryptedPayload = payload,
                createdAtEpochMilliseconds = now,
                expiresAtEpochMilliseconds = now + 60_000L
            )
        val response =
            client.post("/v1/mailboxes/${mailbox.deliveryRoute.mailboxId}/envelopes") {
                bearerAuth(mailbox.deliveryRoute.sendCapability)
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(MailboxEnvelopeRequest(envelope)))
            }
        assertEquals(HttpStatusCode.Accepted, response.status)
    }

    private suspend fun ApplicationTestBuilder.wait(entry: MailboxWaitEntry): List<String> {
        val response =
            client.post("/v1/mailboxes/wait") {
                contentType(ContentType.Application.Json)
                setBody(serverJson.encodeToString(MailboxWaitRequest(listOf(entry))))
            }
        assertEquals(HttpStatusCode.OK, response.status)
        return serverJson.decodeFromString<MailboxWaitResponse>(response.bodyAsText()).ready
    }
}
