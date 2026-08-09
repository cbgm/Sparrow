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
import kotlin.test.Test
import kotlin.test.assertEquals

class NodeRegistryStoreTest {
    @Test
    fun expiredHeartbeatRemovesNodeFromHealthyDirectory() =
        runTest {
            var now = 1_000L
            val identity = NodeIdentity.generate()
            val descriptor =
                ProtocolSignatures.signDescriptor(
                    SecureChatNodeDescriptor(
                        nodeId = identity.nodeId,
                        clientEndpoint = "ws://node/relay",
                        federationEndpoint = "http://node/federation",
                        mailboxEndpoint = "http://node/mailbox",
                        identityPublicKey = identity.encodedPublicKey,
                        protocolVersions = setOf(1),
                        capabilities = NodeCapability.entries.toSet(),
                        validUntilEpochMilliseconds = 100_000L,
                        signature = byteArrayOf()
                    ),
                    identity
                )
            val store = NodeRegistryStore(heartbeatGraceMilliseconds = 100L, now = { now })

            store.register(descriptor)
            assertEquals(listOf(descriptor), store.healthyNodes())

            now += 101L
            assertEquals(emptyList(), store.healthyNodes())
        }

    @Test
    fun heartbeatUpdatesReportedConnectionLoad() =
        runTest {
            val now = 1_000L
            val identity = NodeIdentity.generate()
            val descriptor = descriptor(identity)
            val store = NodeRegistryStore(now = { now })

            store.register(descriptor)
            store.heartbeat(
                signedHeartbeat(
                    identity = identity,
                    activeConnections = 3,
                    now = now
                )
            )

            assertEquals(3, store.healthyNodes().single().activeConnections)
        }

    @Test
    fun heartbeatWithoutLoadPreservesLastReportedConnectionLoad() =
        runTest {
            val now = 1_000L
            val identity = NodeIdentity.generate()
            val store = NodeRegistryStore(now = { now })

            store.register(descriptor(identity))
            store.heartbeat(
                signedHeartbeat(
                    identity = identity,
                    activeConnections = 3,
                    now = now,
                    nonce = "load-3"
                )
            )
            store.heartbeat(
                signedHeartbeat(
                    identity = identity,
                    activeConnections = null,
                    now = now,
                    nonce = "load-unavailable"
                )
            )

            assertEquals(3, store.healthyNodes().single().activeConnections)
        }

    private fun descriptor(identity: NodeIdentity): SecureChatNodeDescriptor =
        ProtocolSignatures.signDescriptor(
            SecureChatNodeDescriptor(
                nodeId = identity.nodeId,
                clientEndpoint = "ws://node/relay",
                federationEndpoint = "http://node/federation",
                mailboxEndpoint = "http://node/mailbox",
                identityPublicKey = identity.encodedPublicKey,
                protocolVersions = setOf(1),
                capabilities = NodeCapability.entries.toSet(),
                validUntilEpochMilliseconds = 100_000L,
                signature = byteArrayOf()
            ),
            identity
        )

    private fun signedHeartbeat(
        identity: NodeIdentity,
        activeConnections: Int?,
        now: Long,
        nonce: String = "nonce-$activeConnections"
    ): NodeHeartbeatRequest {
        val unsigned =
            NodeHeartbeatRequest(
                nodeId = identity.nodeId,
                timestampEpochMilliseconds = now,
                nonce = nonce,
                activeConnections = activeConnections,
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
}
