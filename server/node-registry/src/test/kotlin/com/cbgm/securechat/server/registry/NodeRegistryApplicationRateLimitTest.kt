package com.cbgm.securechat.server.registry

import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.NodeRegistrationRequest
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.ProtocolSignatures
import com.cbgm.securechat.server.security.RateLimitPolicy
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class NodeRegistryApplicationRateLimitTest {
    @Test
    fun differentNodesDoNotShareRegistrationBucket() =
        testApplication {
            val registryIdentity = NodeIdentity.generate()
            application {
                nodeRegistryModule(
                    identity = registryIdentity,
                    config =
                        NodeRegistryConfig(
                            databaseUrl = null,
                            databaseUser = "",
                            databasePassword = "",
                            databaseMaximumPoolSize = 1,
                            supportedProtocolVersions = setOf(1),
                            heartbeatGraceMilliseconds = 90_000L,
                            replayRetentionMilliseconds = 300_000L,
                            registrationRateLimit =
                                RateLimitPolicy(
                                    maximumRequests = 1,
                                    windowMilliseconds = 60_000L
                                )
                        ),
                    store = NodeRegistryStore()
                )
            }

            val nodeA = descriptor(NodeIdentity.generate())
            val nodeB = descriptor(NodeIdentity.generate())

            assertEquals(HttpStatusCode.Created, register(nodeA).status)
            assertEquals(HttpStatusCode.Created, register(nodeB).status)
            assertEquals(HttpStatusCode.TooManyRequests, register(nodeA).status)
        }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.register(
        descriptor: SecureChatNodeDescriptor
    ) = client.post("/v1/nodes") {
        contentType(ContentType.Application.Json)
        setBody(serverJson.encodeToString(NodeRegistrationRequest(descriptor)))
    }

    private fun descriptor(identity: NodeIdentity): SecureChatNodeDescriptor =
        ProtocolSignatures.signDescriptor(
            SecureChatNodeDescriptor(
                nodeId = identity.nodeId,
                clientEndpoint = "wss://${identity.nodeId}/relay",
                federationEndpoint = "https://${identity.nodeId}/federation",
                mailboxEndpoint = "https://${identity.nodeId}/mailbox",
                identityPublicKey = identity.encodedPublicKey,
                protocolVersions = setOf(1),
                capabilities = NodeCapability.entries.toSet(),
                validUntilEpochMilliseconds = System.currentTimeMillis() + 60_000L,
                signature = byteArrayOf()
            ),
            identity
        )
}
