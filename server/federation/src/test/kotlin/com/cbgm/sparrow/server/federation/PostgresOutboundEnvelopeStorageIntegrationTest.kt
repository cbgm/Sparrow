package com.cbgm.sparrow.server.federation

import com.cbgm.sparrow.server.protocol.DeliveryRoute
import com.cbgm.sparrow.server.protocol.EnvelopeAcceptanceState
import com.cbgm.sparrow.server.protocol.FederatedEnvelope
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PostgresOutboundEnvelopeStorageIntegrationTest {
    @Test
    fun pendingEnvelopeAndRetryStateSurviveStorageRecreation() =
        runTest {
            val databaseUrl =
                System
                    .getenv("FEDERATION_TEST_DATABASE_URL")
                    ?.takeIf(String::isNotBlank)
                    ?: return@runTest
            val config = databaseConfig(databaseUrl)
            val currentTime = System.currentTimeMillis()
            val envelope = testEnvelope(currentTime)
            val nextAttemptAt = currentTime + 30_000L

            createOutboundEnvelopeStorage(config).use { storage ->
                storage.enqueue(envelope)
                val attempted = assertNotNull(storage.markAttempt(envelope.envelopeId, nextAttemptAt))
                assertEquals(1, attempted.attempts)
            }

            createOutboundEnvelopeStorage(config).use { storage ->
                val restored = assertNotNull(storage.get(envelope.envelopeId))
                assertEquals(envelope.envelopeId, restored.envelope.envelopeId)
                assertEquals(envelope.encryptedPayload, restored.envelope.encryptedPayload)
                assertContentEquals(
                    envelope.mailboxRoute?.identitySignature,
                    restored.envelope.mailboxRoute?.identitySignature
                )
                assertEquals(EnvelopeAcceptanceState.QUEUED_AT_GATEWAY, restored.state)
                assertEquals(1, restored.attempts)
                assertEquals(nextAttemptAt, restored.nextAttemptAtEpochMilliseconds)
                assertEquals(1, storage.pendingCount())
                storage.markStored(envelope.envelopeId)
            }

            createOutboundEnvelopeStorage(config).use { storage ->
                val restored = assertNotNull(storage.get(envelope.envelopeId))
                assertEquals(EnvelopeAcceptanceState.STORED_AT_DESTINATION, restored.state)
                assertEquals(0, storage.pendingCount())
            }
        }

    private fun databaseConfig(databaseUrl: String): FederationConfig =
        FederationConfig(
            databaseUrl = databaseUrl,
            databaseUser = System.getenv("FEDERATION_TEST_DATABASE_USER") ?: "sparrow_federation",
            databasePassword =
                System.getenv("FEDERATION_TEST_DATABASE_PASSWORD") ?: "local-development-password",
            databaseMaximumPoolSize = 2,
            controlPlaneUrls = emptyList(),
            nodeRegistryUrl = "http://localhost:8090",
            presenceDirectoryUrl = "http://localhost:8091",
            gatewayInternalUrl = "http://localhost:8094",
            federationInternalApiToken = null,
            gatewayInternalApiToken = null,
            maximumDeduplicationEntries = 100,
            registerNode = false,
            clientEndpoint = "ws://localhost:8094/v1/gateway",
            federationEndpoint = "http://localhost:8093",
            mailboxEndpoint = "http://localhost:8092",
            outboundRetryPollIntervalMilliseconds = 1_000L,
            outboundRetryBaseDelayMilliseconds = 5_000L,
            outboundRetryMaximumDelayMilliseconds = 300_000L,
            outboundRetryBatchSize = 100
        )

    private fun testEnvelope(currentTime: Long): FederatedEnvelope =
        FederatedEnvelope(
            envelopeId = "federation-${UUID.randomUUID()}",
            senderRoutingId = "sender",
            recipientDeviceRoutingId = "recipient",
            mailboxRoute =
                DeliveryRoute(
                    routeId = "route",
                    nodeId = "mailbox-node",
                    nodeEndpoint = "http://mailbox",
                    mailboxId = "mailbox",
                    sendCapability = "capability",
                    sequence = 1L,
                    expiresAtEpochMilliseconds = currentTime + 120_000L,
                    identitySignature = byteArrayOf(1)
                ),
            encryptedPayload = "ciphertext",
            createdAtEpochMilliseconds = currentTime,
            expiresAtEpochMilliseconds = currentTime + 120_000L
        )
}
