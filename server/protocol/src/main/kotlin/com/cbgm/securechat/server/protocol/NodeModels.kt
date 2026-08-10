package com.cbgm.securechat.server.protocol

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
) {
    init {
        require(nodeId.isNotBlank())
        require(clientEndpoint.isNotBlank())
        require(federationEndpoint.isNotBlank())
        require(mailboxEndpoint.isNotBlank())
        require(identityPublicKey.isNotEmpty())
        require(protocolVersions.isNotEmpty() && protocolVersions.all { it > 0 })
        require(validUntilEpochMilliseconds > 0L)
        require(activeConnections == null || activeConnections >= 0)
    }
}

@Serializable
data class UnsignedNodeDescriptor(
    val nodeId: String,
    val clientEndpoint: String,
    val federationEndpoint: String,
    val mailboxEndpoint: String,
    val identityPublicKey: ByteArray,
    val protocolVersions: Set<Int>,
    val capabilities: Set<NodeCapability>,
    val validUntilEpochMilliseconds: Long
)

fun SecureChatNodeDescriptor.unsigned(): UnsignedNodeDescriptor =
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
data class NodeRegistrationRequest(
    val descriptor: SecureChatNodeDescriptor
)

@Serializable
data class NodeHeartbeatRequest(
    val nodeId: String,
    val timestampEpochMilliseconds: Long,
    val nonce: String,
    val activeConnections: Int? = null,
    val signature: ByteArray
) {
    init {
        require(activeConnections == null || activeConnections >= 0)
    }
}

@Serializable
data class UnsignedNodeHeartbeat(
    val nodeId: String,
    val timestampEpochMilliseconds: Long,
    val nonce: String,
    val activeConnections: Int?
)

fun NodeHeartbeatRequest.unsigned(): UnsignedNodeHeartbeat =
    UnsignedNodeHeartbeat(
        nodeId = nodeId,
        timestampEpochMilliseconds = timestampEpochMilliseconds,
        nonce = nonce,
        activeConnections = activeConnections
    )

@Serializable
data class RegistryAuthorityCertificate(
    val rootNodeId: String,
    val rootPublicKey: ByteArray,
    val authorityNodeId: String,
    val authorityPublicKey: ByteArray,
    val keyVersion: Long,
    val validFromEpochMilliseconds: Long,
    val validUntilEpochMilliseconds: Long,
    val signature: ByteArray
) {
    init {
        require(rootNodeId.isNotBlank())
        require(rootPublicKey.isNotEmpty())
        require(authorityNodeId.isNotBlank())
        require(authorityPublicKey.isNotEmpty())
        require(keyVersion >= 0L)
        require(validFromEpochMilliseconds < validUntilEpochMilliseconds)
    }
}

@Serializable
data class UnsignedRegistryAuthorityCertificate(
    val rootNodeId: String,
    val rootPublicKey: ByteArray,
    val authorityNodeId: String,
    val authorityPublicKey: ByteArray,
    val keyVersion: Long,
    val validFromEpochMilliseconds: Long,
    val validUntilEpochMilliseconds: Long
)

fun RegistryAuthorityCertificate.unsigned(): UnsignedRegistryAuthorityCertificate =
    UnsignedRegistryAuthorityCertificate(
        rootNodeId = rootNodeId,
        rootPublicKey = rootPublicKey,
        authorityNodeId = authorityNodeId,
        authorityPublicKey = authorityPublicKey,
        keyVersion = keyVersion,
        validFromEpochMilliseconds = validFromEpochMilliseconds,
        validUntilEpochMilliseconds = validUntilEpochMilliseconds
    )

@Serializable
data class RegistrySigningCertificate(
    val authorityNodeId: String,
    val authorityPublicKey: ByteArray,
    val signingNodeId: String,
    val signingPublicKey: ByteArray,
    val keyVersion: Long,
    val validFromEpochMilliseconds: Long,
    val validUntilEpochMilliseconds: Long,
    val signature: ByteArray
) {
    init {
        require(authorityNodeId.isNotBlank())
        require(authorityPublicKey.isNotEmpty())
        require(signingNodeId.isNotBlank())
        require(signingPublicKey.isNotEmpty())
        require(keyVersion >= 0L)
        require(validFromEpochMilliseconds < validUntilEpochMilliseconds)
    }
}

@Serializable
data class UnsignedRegistrySigningCertificate(
    val authorityNodeId: String,
    val authorityPublicKey: ByteArray,
    val signingNodeId: String,
    val signingPublicKey: ByteArray,
    val keyVersion: Long,
    val validFromEpochMilliseconds: Long,
    val validUntilEpochMilliseconds: Long
)

fun RegistrySigningCertificate.unsigned(): UnsignedRegistrySigningCertificate =
    UnsignedRegistrySigningCertificate(
        authorityNodeId = authorityNodeId,
        authorityPublicKey = authorityPublicKey,
        signingNodeId = signingNodeId,
        signingPublicKey = signingPublicKey,
        keyVersion = keyVersion,
        validFromEpochMilliseconds = validFromEpochMilliseconds,
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
    val authorityCertificate: RegistryAuthorityCertificate? = null,
    val signingCertificate: RegistrySigningCertificate? = null,
    val signature: ByteArray
)
