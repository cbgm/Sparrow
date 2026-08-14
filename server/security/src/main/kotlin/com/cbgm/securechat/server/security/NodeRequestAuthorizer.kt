package com.cbgm.securechat.server.security

import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import io.ktor.server.application.ApplicationCall

class NodeRequestAuthorizer(
    private val descriptorResolver: NodeDescriptorResolver,
    private val requestVerifier: NodeRequestVerifier = NodeRequestVerifier(),
    private val now: () -> Long = System::currentTimeMillis
) {
    suspend fun isAuthorized(
        authentication: NodeRequestAuthentication?,
        method: String,
        path: String,
        body: String,
        requirements: NodeRequestAuthorizationRequirements =
            NodeRequestAuthorizationRequirements()
    ): Boolean =
        runCatching {
            authentication?.let { authenticationValue ->
                descriptorResolver.resolve(authenticationValue.nodeId)?.let { descriptor ->
                    val expectedNodeId = requirements.expectedNodeId
                    val expectedNodeMatches =
                        expectedNodeId == null || expectedNodeId == authenticationValue.nodeId
                    val requiredCapability = requirements.requiredCapability
                    val requiredCapabilityPresent =
                        requiredCapability == null || requiredCapability in descriptor.capabilities
                    val descriptorIsUsable =
                        descriptor.validUntilEpochMilliseconds > now() &&
                            ProtocolSignatures.verifyDescriptor(descriptor)
                    val requestIsValid =
                        requestVerifier.verify(
                            authentication = authenticationValue,
                            method = method,
                            path = path,
                            body = body,
                            publicKey = Signatures.decodePublicKey(descriptor.identityPublicKey)
                        )

                    listOf(
                        descriptor.nodeId == authenticationValue.nodeId,
                        expectedNodeMatches,
                        requiredCapabilityPresent,
                        descriptorIsUsable,
                        requestIsValid
                    ).all { condition -> condition }
                } ?: false
            } ?: false
        }.getOrDefault(false)
}

fun interface NodeDescriptorResolver {
    suspend fun resolve(nodeId: String): SecureChatNodeDescriptor?
}

data class NodeRequestAuthorizationRequirements(
    val expectedNodeId: String? = null,
    val requiredCapability: NodeCapability? = null
)

fun ApplicationCall.nodeRequestAuthentication(): NodeRequestAuthentication? {
    val nodeId = request.headers[NodeRequestHeaders.NODE_ID]
    val timestamp = request.headers[NodeRequestHeaders.TIMESTAMP]?.toLongOrNull()
    val nonce = request.headers[NodeRequestHeaders.NONCE]
    val signature = request.headers[NodeRequestHeaders.SIGNATURE]

    val textHeadersPresent = listOf(nodeId, nonce, signature).all { value -> !value.isNullOrBlank() }
    return if (textHeadersPresent && timestamp != null) {
        NodeRequestAuthentication(
            nodeId = requireNotNull(nodeId),
            timestampEpochMilliseconds = timestamp,
            nonce = requireNotNull(nonce),
            signature = requireNotNull(signature)
        )
    } else {
        null
    }
}
