package com.cbgm.sparrow.server.security

import com.cbgm.sparrow.server.protocol.NodeCapability
import com.cbgm.sparrow.server.protocol.SparrowNodeDescriptor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeRequestAuthorizerTest {
    @Test
    fun acceptsSignedRequestFromRegisteredNode() =
        runTest {
            val fixture = fixture()
            val authentication = fixture.signer.sign("POST", REQUEST_PATH, REQUEST_BODY)

            assertTrue(
                fixture.authorizer.isAuthorized(
                    authentication = authentication,
                    method = "POST",
                    path = REQUEST_PATH,
                    body = REQUEST_BODY,
                    requirements =
                        NodeRequestAuthorizationRequirements(
                            expectedNodeId = fixture.identity.nodeId,
                            requiredCapability = NodeCapability.GATEWAY
                        )
                )
            )
        }

    @Test
    fun rejectsReplayAndWrongCapability() =
        runTest {
            val fixture = fixture(capabilities = setOf(NodeCapability.GATEWAY))
            val authentication = fixture.signer.sign("POST", REQUEST_PATH, REQUEST_BODY)

            assertTrue(
                fixture.authorizer.isAuthorized(
                    authentication = authentication,
                    method = "POST",
                    path = REQUEST_PATH,
                    body = REQUEST_BODY,
                    requirements =
                        NodeRequestAuthorizationRequirements(
                            requiredCapability = NodeCapability.GATEWAY
                        )
                )
            )
            assertFalse(
                fixture.authorizer.isAuthorized(
                    authentication = authentication,
                    method = "POST",
                    path = REQUEST_PATH,
                    body = REQUEST_BODY,
                    requirements =
                        NodeRequestAuthorizationRequirements(
                            requiredCapability = NodeCapability.GATEWAY
                        )
                )
            )
            assertFalse(
                fixture.authorizer.isAuthorized(
                    authentication = fixture.signer.sign("POST", REQUEST_PATH, REQUEST_BODY),
                    method = "POST",
                    path = REQUEST_PATH,
                    body = REQUEST_BODY,
                    requirements =
                        NodeRequestAuthorizationRequirements(
                            requiredCapability = NodeCapability.MAILBOX
                        )
                )
            )
        }

    @Test
    fun rejectsExpiredDescriptor() =
        runTest {
            val fixture = fixture(validUntil = CURRENT_TIME)

            assertFalse(
                fixture.authorizer.isAuthorized(
                    authentication = fixture.signer.sign("POST", REQUEST_PATH, REQUEST_BODY),
                    method = "POST",
                    path = REQUEST_PATH,
                    body = REQUEST_BODY
                )
            )
        }

    private fun fixture(
        capabilities: Set<NodeCapability> = NodeCapability.entries.toSet(),
        validUntil: Long = CURRENT_TIME + 60_000L
    ): AuthorizationFixture {
        val identity = NodeIdentity.generate()
        val descriptor =
            ProtocolSignatures.signDescriptor(
                SparrowNodeDescriptor(
                    nodeId = identity.nodeId,
                    clientEndpoint = "wss://node.example/v1/gateway",
                    federationEndpoint = "https://node.example",
                    mailboxEndpoint = "https://node.example",
                    identityPublicKey = identity.encodedPublicKey,
                    protocolVersions = setOf(1),
                    capabilities = capabilities,
                    validUntilEpochMilliseconds = validUntil,
                    signature = byteArrayOf()
                ),
                identity
            )
        val requestVerifier =
            NodeRequestVerifier(
                ReplayProtection(now = { CURRENT_TIME })
            )
        return AuthorizationFixture(
            identity = identity,
            signer = NodeRequestSigner(identity, now = { CURRENT_TIME }),
            authorizer =
                NodeRequestAuthorizer(
                    descriptorResolver = NodeDescriptorResolver { descriptor },
                    requestVerifier = requestVerifier,
                    now = { CURRENT_TIME }
                )
        )
    }

    private data class AuthorizationFixture(
        val identity: NodeIdentity,
        val signer: NodeRequestSigner,
        val authorizer: NodeRequestAuthorizer
    )

    private companion object {
        const val CURRENT_TIME = 1_000_000L
        const val REQUEST_PATH = "/v1/node-push/envelopes"
        const val REQUEST_BODY = "{}"
    }
}
