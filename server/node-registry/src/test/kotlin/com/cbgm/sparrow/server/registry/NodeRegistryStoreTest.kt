package com.cbgm.sparrow.server.registry

import com.cbgm.sparrow.server.protocol.NodeCapability
import com.cbgm.sparrow.server.protocol.NodeHeartbeatRequest
import com.cbgm.sparrow.server.protocol.SparrowNodeDescriptor
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.protocol.unsigned
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.ProtocolSignatures
import com.cbgm.sparrow.server.security.Signatures
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
                    SparrowNodeDescriptor(
                        nodeId = identity.nodeId,
                        clientEndpoint = "ws://node/v1/gateway",
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
    fun aSignedHeartbeatKeepsTheNodeListed() =
        runTest {
            var now = 1_000L
            val identity = NodeIdentity.generate()
            val store = NodeRegistryStore(heartbeatGraceMilliseconds = 100L, now = { now })

            store.register(descriptor(identity))
            now += 90L
            assertEquals(RegistrationResult.Accepted, store.heartbeat(signedHeartbeat(identity = identity, now = now)))
            now += 60L

            assertEquals(identity.nodeId, store.healthyNodes().single().nodeId)
        }

    private fun descriptor(identity: NodeIdentity): SparrowNodeDescriptor =
        ProtocolSignatures.signDescriptor(
            SparrowNodeDescriptor(
                nodeId = identity.nodeId,
                clientEndpoint = "ws://node/v1/gateway",
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
        now: Long,
        nonce: String = "nonce"
    ): NodeHeartbeatRequest {
        val unsigned =
            NodeHeartbeatRequest(
                nodeId = identity.nodeId,
                timestampEpochMilliseconds = now,
                nonce = nonce,
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
