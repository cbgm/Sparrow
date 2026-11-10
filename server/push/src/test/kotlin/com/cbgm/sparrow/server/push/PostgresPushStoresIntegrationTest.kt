package com.cbgm.sparrow.server.push

import com.cbgm.sparrow.server.protocol.TransportEnvelope
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostgresPushStoresIntegrationTest {
    @Test
    fun dataSurvivesStoreRecreation() =
        runTest {
            val databaseUrl =
                System
                    .getenv("PUSH_TEST_DATABASE_URL")
                    ?.takeIf(String::isNotBlank)
                    ?: return@runTest
            val suffix = UUID.randomUUID().toString()
            val routingId = "routing-$suffix"
            val envelope =
                TransportEnvelope(
                    envelopeId = "envelope-$suffix",
                    senderId = "sender-$suffix",
                    recipientId = routingId,
                    payload = "ciphertext",
                    createdAtEpochMilliseconds = System.currentTimeMillis()
                )
            val config = databaseConfig(databaseUrl)
            createPostgresPushStores(config).use { stores ->
                assertTrue(stores.pendingEnvelopes.enqueue(envelope))
            }

            createPostgresPushStores(config).use { stores ->
                assertEquals(listOf(envelope), stores.pendingEnvelopes.pending(routingId))
                assertEquals(setOf(routingId), stores.pendingEnvelopes.pendingRecipientIds())

                stores.pendingEnvelopes.remove(routingId, envelope.envelopeId)
            }
        }

    private fun databaseConfig(databaseUrl: String): PushConfig =
        PushConfig(
            pushInternalApiToken = null,
            nodeRegistryUrl = null,
            presenceDirectoryUrl = null,
            databaseUrl = databaseUrl,
            databaseUser = System.getenv("PUSH_TEST_DATABASE_USER") ?: "sparrow_push",
            databasePassword =
                System.getenv("PUSH_TEST_DATABASE_PASSWORD") ?: "local-development-password",
            databaseMaximumPoolSize = 2,
            maximumEnvelopes = 100,
            envelopeRetentionMilliseconds = 60_000L
        )
}
