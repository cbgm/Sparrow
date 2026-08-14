package com.cbgm.securechat.server.mailbox

import com.cbgm.securechat.server.protocol.CreateMailboxRequest
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MailboxStoreTest {
    @Test
    fun creationEnforcesPerOwnerAndGlobalQuotas() =
        runTest {
            val store = MailboxStore(now = { 1_000L })
            val request =
                CreateMailboxRequest(
                    nodeId = "node-a",
                    nodeEndpoint = "http://mailbox",
                    expiresAtEpochMilliseconds = 10_000L
                )

            assertIs<MailboxCreationResult.Created>(
                store.createWithQuota(request, "owner-a", 2, 1)
            )
            assertEquals(
                MailboxCreationResult.OwnerQuotaExceeded,
                store.createWithQuota(request, "owner-a", 2, 1)
            )
            assertIs<MailboxCreationResult.Created>(
                store.createWithQuota(request, "owner-b", 2, 1)
            )
            assertEquals(
                MailboxCreationResult.GlobalQuotaExceeded,
                store.createWithQuota(request, "owner-c", 2, 1)
            )
        }

    @Test
    fun retrievalCapabilityRevokesMailboxAndPendingEnvelopes() =
        runTest {
            val store = MailboxStore(now = { 1_000L })
            val mailbox =
                store.create(
                    CreateMailboxRequest(
                        nodeId = "node-a",
                        nodeEndpoint = "http://mailbox",
                        expiresAtEpochMilliseconds = 10_000L
                    )
                )

            assertEquals(
                MailboxRevocationResult.Unauthorized,
                store.revoke(mailbox.deliveryRoute.mailboxId, "wrong-capability")
            )
            assertEquals(1, store.mailboxCount())
            assertEquals(
                MailboxRevocationResult.Revoked,
                store.revoke(
                    mailbox.deliveryRoute.mailboxId,
                    mailbox.retrievalCapability
                )
            )
            assertEquals(0, store.mailboxCount())
            assertNull(
                store.pending(
                    mailbox.deliveryRoute.mailboxId,
                    mailbox.retrievalCapability
                )
            )
            assertEquals(
                MailboxRevocationResult.NotFound,
                store.revoke(
                    mailbox.deliveryRoute.mailboxId,
                    mailbox.retrievalCapability
                )
            )
        }

    @Test
    fun envelopeIsStoredIdempotentlyUntilProcessedAcknowledgement() =
        runTest {
            val store = MailboxStore(now = { 1_000L })
            val mailbox =
                store.create(
                    CreateMailboxRequest(
                        nodeId = "node-a",
                        nodeEndpoint = "http://mailbox",
                        expiresAtEpochMilliseconds = 10_000L
                    )
                )
            val route = mailbox.deliveryRoute
            val envelope =
                FederatedEnvelope(
                    envelopeId = "envelope-1",
                    senderRoutingId = "sender",
                    recipientDeviceRoutingId = "recipient",
                    mailboxRoute = route,
                    encryptedPayload = "ciphertext",
                    createdAtEpochMilliseconds = 1_000L,
                    expiresAtEpochMilliseconds = 9_000L
                )

            assertIs<MailboxResult.Stored>(store.store(route.mailboxId, route.sendCapability, envelope))
            val duplicate =
                assertIs<MailboxResult.Stored>(
                    store.store(route.mailboxId, route.sendCapability, envelope)
                )
            assertTrue(duplicate.duplicate)
            assertEquals(listOf(envelope), store.pending(route.mailboxId, mailbox.retrievalCapability))
            assertTrue(
                store.acknowledge(
                    route.mailboxId,
                    mailbox.retrievalCapability,
                    envelope.envelopeId
                )
            )
            assertEquals(emptyList(), store.pending(route.mailboxId, mailbox.retrievalCapability))
        }
}
