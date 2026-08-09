package com.cbgm.securechat.feature.transport.discovery

import kotlinx.serialization.Serializable

@Serializable
enum class NodeCapability {
    GATEWAY,
    FEDERATION,
    MAILBOX
}

@Serializable
data class SecureChatNodeDescriptor(
    val nodeId: String,
    val clientEndpoint: String,
    val federationEndpoint: String,
    val mailboxEndpoint: String,
    val identityPublicKey: ByteArray,
    val protocolVersions: Set<Int>,
    val capabilities: Set<NodeCapability>,
    val validUntilEpochMilliseconds: Long,
    val activeConnections: Int? = null,
    val signature: ByteArray
)

@Serializable
internal data class UnsignedNodeDescriptor(
    val nodeId: String,
    val clientEndpoint: String,
    val federationEndpoint: String,
    val mailboxEndpoint: String,
    val identityPublicKey: ByteArray,
    val protocolVersions: Set<Int>,
    val capabilities: Set<NodeCapability>,
    val validUntilEpochMilliseconds: Long
)

internal fun SecureChatNodeDescriptor.unsigned(): UnsignedNodeDescriptor =
    UnsignedNodeDescriptor(
        nodeId = nodeId,
        clientEndpoint = clientEndpoint,
        federationEndpoint = federationEndpoint,
        mailboxEndpoint = mailboxEndpoint,
        identityPublicKey = identityPublicKey,
        protocolVersions = protocolVersions,
        capabilities = capabilities,
        validUntilEpochMilliseconds = validUntilEpochMilliseconds
    )

@Serializable
data class NodeDirectory(
    val generatedAtEpochMilliseconds: Long,
    val validUntilEpochMilliseconds: Long,
    val nodes: List<SecureChatNodeDescriptor>
)

@Serializable
data class SignedNodeDirectory(
    val directory: NodeDirectory,
    val authorityNodeId: String,
    val authorityPublicKey: ByteArray,
    val signature: ByteArray
)

data class NodeEndpoint(
    val nodeId: String,
    val websocketUrl: String,
    val mailboxRouteEndpoint: String? = null,
    val mailboxAccessEndpoint: String? = null,
    val activeConnections: Int = 0
)
