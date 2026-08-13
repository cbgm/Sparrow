package com.cbgm.securechat.server.push

import com.cbgm.securechat.server.protocol.TransportEnvelope
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
            val token = "token-$suffix"
            val envelope =
                TransportEnvelope(
                    envelopeId = "envelope-$suffix",
                    senderId = "sender-$suffix",
                    recipientId = routingId,
                    payload = "ciphertext",
                    createdAtEpochMilliseconds = System.currentTimeMillis()
                )
            val config = databaseConfig(databaseUrl)
            val wakeUpId =
                createPostgresPushStores(config).use { stores ->
                    stores.devices.register(PushDevice(routingId, token, "ANDROID"))
                    assertTrue(stores.pendingEnvelopes.enqueue(envelope))
                    stores.wakeUps.create(routingId)
                }

            createPostgresPushStores(config).use { stores ->
                assertEquals(
                    listOf(PushDevice(routingId, token, "ANDROID")),
                    stores.devices.find(routingId)
                )
                assertEquals(listOf(envelope), stores.pendingEnvelopes.pending(routingId))
                assertEquals(setOf(routingId), stores.pendingEnvelopes.pendingRecipientIds())
                assertEquals(routingId, stores.wakeUps.resolve(wakeUpId))

                stores.devices.removeToken(token)
                stores.pendingEnvelopes.remove(routingId, envelope.envelopeId)
            }
        }

    private fun databaseConfig(databaseUrl: String): PushConfig =
        PushConfig(
            pushInternalApiToken = null,
            nodeRegistryUrl = null,
            presenceDirectoryUrl = null,
            databaseUrl = databaseUrl,
            databaseUser = System.getenv("PUSH_TEST_DATABASE_USER") ?: "securechat_push",
            databasePassword =
                System.getenv("PUSH_TEST_DATABASE_PASSWORD") ?: "local-development-password",
            databaseMaximumPoolSize = 2,
            maximumEnvelopes = 100,
            envelopeRetentionMilliseconds = 60_000L,
            wakeUpLifetimeMilliseconds = 60_000L
        )
}
