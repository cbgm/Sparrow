package com.cbgm.securechat.server.mailbox

import com.cbgm.securechat.server.protocol.CreateMailboxRequest
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostgresMailboxStoreIntegrationTest {
    @Test
    fun perOwnerQuotaSurvivesStoreRecreation() =
        runTest {
            val databaseUrl =
                System
                    .getenv("MAILBOX_TEST_DATABASE_URL")
                    ?.takeIf(String::isNotBlank)
                    ?: return@runTest
            val now = System.currentTimeMillis()
            val suffix = UUID.randomUUID().toString()
            val ownerKeyHash = "owner-$suffix"
            val config = databaseConfig(databaseUrl)
            val request =
                CreateMailboxRequest(
                    nodeId = "node-$suffix",
                    nodeEndpoint = "http://mailbox",
                    expiresAtEpochMilliseconds = now + 60_000L
                )
            val created =
                createMailboxStorage(config).use { store ->
                    assertIs<MailboxCreationResult.Created>(
                        store.createWithQuota(
                            request = request,
                            ownerKeyHash = ownerKeyHash,
                            maximumMailboxes = Int.MAX_VALUE,
                            maximumMailboxesPerOwner = 1
                        )
                    ).response
                }

            createMailboxStorage(config).use { store ->
                assertEquals(
                    MailboxCreationResult.OwnerQuotaExceeded,
                    store.createWithQuota(
                        request = request,
                        ownerKeyHash = ownerKeyHash,
                        maximumMailboxes = Int.MAX_VALUE,
                        maximumMailboxesPerOwner = 1
                    )
                )
                assertEquals(
                    MailboxRevocationResult.Revoked,
                    store.revoke(
                        created.deliveryRoute.mailboxId,
                        created.retrievalCapability
                    )
                )
            }
        }

    @Test
    fun mailboxAndEnvelopeSurviveStoreRecreation() =
        runTest {
            val databaseUrl =
                System
                    .getenv("MAILBOX_TEST_DATABASE_URL")
                    ?.takeIf(String::isNotBlank)
                    ?: return@runTest
            val now = System.currentTimeMillis()
            val suffix = UUID.randomUUID().toString()
            val config = databaseConfig(databaseUrl)
            val mailbox =
                createMailboxStorage(config).use { store ->
                    val created =
                        store.create(
                            CreateMailboxRequest(
                                nodeId = "node-$suffix",
                                nodeEndpoint = "http://mailbox",
                                expiresAtEpochMilliseconds = now + 60_000L
                            )
                        )
                    val envelope =
                        FederatedEnvelope(
                            envelopeId = "envelope-$suffix",
                            senderRoutingId = "sender-$suffix",
                            recipientDeviceRoutingId = "recipient-$suffix",
                            mailboxRoute = created.deliveryRoute,
                            encryptedPayload = "ciphertext",
                            createdAtEpochMilliseconds = now,
                            expiresAtEpochMilliseconds = now + 30_000L
                        )
                    assertIs<MailboxResult.Stored>(
                        store.store(
                            created.deliveryRoute.mailboxId,
                            created.deliveryRoute.sendCapability,
                            envelope
                        )
                    )
                    created to envelope
                }

            createMailboxStorage(config).use { store ->
                val (created, envelope) = mailbox
                assertEquals(
                    listOf(envelope),
                    store.pending(created.deliveryRoute.mailboxId, created.retrievalCapability)
                )
                assertTrue(
                    store.acknowledge(
                        created.deliveryRoute.mailboxId,
                        created.retrievalCapability,
                        envelope.envelopeId
                    )
                )
                assertEquals(
                    emptyList(),
                    store.pending(created.deliveryRoute.mailboxId, created.retrievalCapability)
                )
                assertEquals(
                    MailboxRevocationResult.Revoked,
                    store.revoke(
                        created.deliveryRoute.mailboxId,
                        created.retrievalCapability
                    )
                )
                assertNull(
                    store.pending(
                        created.deliveryRoute.mailboxId,
                        created.retrievalCapability
                    )
                )
            }
        }

    private fun databaseConfig(databaseUrl: String): MailboxConfig =
        MailboxConfig(
            databaseUrl = databaseUrl,
            databaseUser = System.getenv("MAILBOX_TEST_DATABASE_USER") ?: "securechat_mailbox",
            databasePassword =
                System.getenv("MAILBOX_TEST_DATABASE_PASSWORD") ?: "local-development-password",
            databaseMaximumPoolSize = 2,
            maximumEnvelopeBytes = 1_048_576,
            maximumMailboxBytes = 100L * 1_048_576L
        )
}
