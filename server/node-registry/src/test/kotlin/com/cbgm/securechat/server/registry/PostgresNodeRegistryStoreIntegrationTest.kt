package com.cbgm.securechat.server.registry

import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.NodeHeartbeatRequest
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.protocol.unsigned
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.ProtocolSignatures
import com.cbgm.securechat.server.security.Signatures
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PostgresNodeRegistryStoreIntegrationTest {
    @Test
    fun descriptorAndHeartbeatReplayProtectionSurviveStoreRecreation() =
        runTest {
            val databaseUrl =
                System.getenv("NODE_REGISTRY_TEST_DATABASE_URL")
                    ?.takeIf(String::isNotBlank)
                    ?: return@runTest
            val identity = NodeIdentity.generate()
            val now = System.currentTimeMillis()
            val descriptor = descriptor(identity, now)
            val heartbeat = heartbeat(identity, now)
            val config = databaseConfig(databaseUrl)

            createNodeRegistryStorage(config).use { store ->
                assertIs<RegistrationResult.Accepted>(store.register(descriptor))
                assertIs<RegistrationResult.Accepted>(store.heartbeat(heartbeat))
            }

            createNodeRegistryStorage(config).use { store ->
                assertEquals(listOf(descriptor), store.healthyNodes())
                assertEquals(descriptor, store.findHealthy(descriptor.nodeId))
                val replay = assertIs<RegistrationResult.Rejected>(store.heartbeat(heartbeat))
                assertEquals("STALE_OR_REPLAYED_HEARTBEAT", replay.code)
            }
        }

    private fun descriptor(
        identity: NodeIdentity,
        now: Long
    ): SecureChatNodeDescriptor =
        ProtocolSignatures.signDescriptor(
            SecureChatNodeDescriptor(
                nodeId = identity.nodeId,
                clientEndpoint = "ws://node-${UUID.randomUUID()}/relay",
                federationEndpoint = "http://node/federation",
                mailboxEndpoint = "http://node/mailbox",
                identityPublicKey = identity.encodedPublicKey,
                protocolVersions = setOf(1),
                capabilities = NodeCapability.entries.toSet(),
                validUntilEpochMilliseconds = now + 120_000L,
                signature = byteArrayOf()
            ),
            identity
        )

    private fun heartbeat(
        identity: NodeIdentity,
        now: Long
    ): NodeHeartbeatRequest {
        val unsigned =
            NodeHeartbeatRequest(
                nodeId = identity.nodeId,
                timestampEpochMilliseconds = now,
                nonce = UUID.randomUUID().toString(),
                signature = byteArrayOf()
            )
        return unsigned.copy(
            signature =
                Signatures.sign(
                    serverJson.encodeToString(unsigned.unsigned()).encodeToByteArray(),
                    identity.privateKey
                )
        )
    }

    private fun databaseConfig(databaseUrl: String): NodeRegistryConfig =
        NodeRegistryConfig(
            databaseUrl = databaseUrl,
            databaseUser =
                System.getenv("NODE_REGISTRY_TEST_DATABASE_USER") ?: "securechat_registry",
            databasePassword =
                System.getenv("NODE_REGISTRY_TEST_DATABASE_PASSWORD")
                    ?: "local-development-password",
            databaseMaximumPoolSize = 2,
            supportedProtocolVersions = setOf(1),
            heartbeatGraceMilliseconds = 90_000L,
            replayRetentionMilliseconds = 5L * 60L * 1_000L
        )
}
