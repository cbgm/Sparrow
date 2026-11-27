package com.cbgm.sparrow.server.mailbox

import com.cbgm.sparrow.server.protocol.CreateMailboxRequest
import com.cbgm.sparrow.server.protocol.CreateMailboxResponse
import com.cbgm.sparrow.server.protocol.DeliveryRoute
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import com.cbgm.sparrow.server.protocol.MailboxCreationWork
import com.cbgm.sparrow.server.protocol.MailboxEnvelopeRequest
import com.cbgm.sparrow.server.protocol.serverJson
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InvitationMailboxTest {
    @Test
    fun anInvitationMailboxTakesOnlyPaidDeposits() =
        runTest {
            val store = MailboxStore(now = { NOW })
            val route = store.create(request(invitation = true)).deliveryRoute

            assertEquals(MailboxResult.Rejected("DEPOSIT_WORK_REQUIRED"), store.store(route.mailboxId, route.sendCapability, envelope(route, "a")))
            assertIs<MailboxResult.Stored>(store.store(route.mailboxId, route.sendCapability, envelope(route, "a"), depositPaid = true))
        }

    @Test
    fun anOrdinaryMailboxNeedsNoWork() =
        runTest {
            val store = MailboxStore(now = { NOW })
            val route = store.create(request(invitation = false)).deliveryRoute

            assertIs<MailboxResult.Stored>(store.store(route.mailboxId, route.sendCapability, envelope(route, "a")))
        }

    @Test
    fun anInvitationMailboxHoldsAFewSmallEnvelopes() =
        runTest {
            val store = MailboxStore(now = { NOW })
            val route = store.create(request(invitation = true)).deliveryRoute
            val tooLarge = envelope(route, "large", payload = "x".repeat(InvitationMailboxes.MAXIMUM_ENVELOPE_BYTES.toInt() + 1))

            assertEquals(MailboxResult.Rejected("ENVELOPE_TOO_LARGE"), store.store(route.mailboxId, route.sendCapability, tooLarge, depositPaid = true))
            repeat(InvitationMailboxes.MAXIMUM_ENVELOPES) { index ->
                assertIs<MailboxResult.Stored>(store.store(route.mailboxId, route.sendCapability, envelope(route, "e$index"), depositPaid = true))
            }
            assertEquals(
                MailboxResult.Rejected("MAILBOX_QUOTA_EXCEEDED"),
                store.store(route.mailboxId, route.sendCapability, envelope(route, "one-more"), depositPaid = true)
            )
            assertEquals(
                MailboxResult.Stored(duplicate = true),
                store.store(route.mailboxId, route.sendCapability, envelope(route, "e0"), depositPaid = true)
            )
        }

    @Test
    fun aDepositIsPaidWithAProofOfWork() =
        testApplication {
            application { mailboxModule(config = config(), store = MailboxStore()) }
            val created = createInvitationMailbox()
            val route = created.deliveryRoute

            assertEquals(HttpStatusCode.BadRequest, deposit(route, "a", work = null).status)
            val work = solvedWork(BITS)
            assertEquals(HttpStatusCode.Accepted, deposit(route, "b", work).status)
            // A stamp pays once.
            assertEquals(HttpStatusCode.BadRequest, deposit(route, "c", work).status)
        }

    private suspend fun ApplicationTestBuilder.createInvitationMailbox(): CreateMailboxResponse {
        val response =
            client.post("/v1/mailboxes") {
                contentType(ContentType.Application.Json)
                setBody(
                    serverJson.encodeToString(
                        request(invitation = true).copy(
                            expiresAtEpochMilliseconds = System.currentTimeMillis() + 60_000L,
                            creationWork = solvedWork(BITS)
                        )
                    )
                )
            }
        return serverJson.decodeFromString(response.bodyAsText())
    }

    private suspend fun ApplicationTestBuilder.deposit(
        route: DeliveryRoute,
        envelopeId: String,
        work: MailboxCreationWork?
    ): HttpResponse =
        client.post("/v1/mailboxes/${route.mailboxId}/envelopes") {
            bearerAuth(route.sendCapability)
            contentType(ContentType.Application.Json)
            val now = System.currentTimeMillis()
            setBody(serverJson.encodeToString(MailboxEnvelopeRequest(envelope(route, envelopeId, createdAt = now), work)))
        }

    private fun request(invitation: Boolean) =
        CreateMailboxRequest(nodeId = "node-a", nodeEndpoint = "http://mailbox", expiresAtEpochMilliseconds = NOW + 60_000L, invitation = invitation)

    private fun envelope(
        route: DeliveryRoute,
        envelopeId: String,
        payload: String = "sealed",
        createdAt: Long = NOW
    ) = FederatedEnvelope(
        envelopeId = envelopeId,
        senderRoutingId = "mailbox",
        recipientDeviceRoutingId = "mailbox",
        mailboxRoute = route,
        encryptedPayload = payload,
        createdAtEpochMilliseconds = createdAt,
        expiresAtEpochMilliseconds = createdAt + 60_000L
    )

    private fun config() =
        MailboxConfig(
            databaseUrl = null,
            databaseUser = "",
            databasePassword = "",
            databaseMaximumPoolSize = 1,
            maximumEnvelopeBytes = 1_048_576,
            maximumMailboxBytes = 100L * 1_048_576L,
            creationWorkBits = BITS
        )

    private companion object {
        const val NOW = 1_000L
        const val BITS = 8
    }
}
