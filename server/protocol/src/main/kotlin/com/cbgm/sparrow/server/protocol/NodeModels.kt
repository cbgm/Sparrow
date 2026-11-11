package com.cbgm.sparrow.server.protocol

import kotlinx.serialization.Serializable

@Serializable
enum class NodeCapability {
    GATEWAY,
    FEDERATION,
    MAILBOX
}

@Serializable
data class SparrowNodeDescriptor(
    val nodeId: String,
    val clientEndpoint: String,
    val federationEndpoint: String,
    val mailboxEndpoint: String,
    val identityPublicKey: ByteArray,
    val protocolVersions: Set<Int>,
    val capabilities: Set<NodeCapability>,
    val validUntilEpochMilliseconds: Long,
    val activeConnections: Int? = null,
    /** Raw X25519 key the node relays onion layers with ([RelayLayer]); null for nodes that do not relay. */
    val relayKey: ByteArray? = null,
    /**
     * Who runs the node, as its operator declares it (`NODE_OPERATOR`); null when not declared. Apps never put two hops
     * of one operator into a circuit. Signed with the rest; without it the signed content is the same as before.
     */
    val operator: String? = null,
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
        require(operator == null || operator.isNotBlank() && operator.length <= MAXIMUM_OPERATOR_LENGTH)
    }

    companion object {
        const val MAXIMUM_OPERATOR_LENGTH = 64
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SparrowNodeDescriptor

        if (validUntilEpochMilliseconds != other.validUntilEpochMilliseconds) return false
        if (activeConnections != other.activeConnections) return false
        if (nodeId != other.nodeId) return false
        if (clientEndpoint != other.clientEndpoint) return false
        if (federationEndpoint != other.federationEndpoint) return false
        if (mailboxEndpoint != other.mailboxEndpoint) return false
        if (!identityPublicKey.contentEquals(other.identityPublicKey)) return false
        if (protocolVersions != other.protocolVersions) return false
        if (capabilities != other.capabilities) return false
        if (!relayKey.contentEquals(other.relayKey)) return false
        if (operator != other.operator) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = validUntilEpochMilliseconds.hashCode()
        result = 31 * result + (activeConnections ?: 0)
        result = 31 * result + nodeId.hashCode()
        result = 31 * result + clientEndpoint.hashCode()
        result = 31 * result + federationEndpoint.hashCode()
        result = 31 * result + mailboxEndpoint.hashCode()
        result = 31 * result + identityPublicKey.contentHashCode()
        result = 31 * result + protocolVersions.hashCode()
        result = 31 * result + capabilities.hashCode()
        result = 31 * result + (relayKey?.contentHashCode() ?: 0)
        result = 31 * result + (operator?.hashCode() ?: 0)
        result = 31 * result + signature.contentHashCode()
        return result
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
    val validUntilEpochMilliseconds: Long,
    val relayKey: ByteArray? = null,
    val operator: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UnsignedNodeDescriptor

        if (validUntilEpochMilliseconds != other.validUntilEpochMilliseconds) return false
        if (nodeId != other.nodeId) return false
        if (clientEndpoint != other.clientEndpoint) return false
        if (federationEndpoint != other.federationEndpoint) return false
        if (mailboxEndpoint != other.mailboxEndpoint) return false
        if (!identityPublicKey.contentEquals(other.identityPublicKey)) return false
        if (protocolVersions != other.protocolVersions) return false
        if (capabilities != other.capabilities) return false
        if (!relayKey.contentEquals(other.relayKey)) return false
        if (operator != other.operator) return false

        return true
    }

    override fun hashCode(): Int {
        var result = validUntilEpochMilliseconds.hashCode()
        result = 31 * result + nodeId.hashCode()
        result = 31 * result + clientEndpoint.hashCode()
        result = 31 * result + federationEndpoint.hashCode()
        result = 31 * result + mailboxEndpoint.hashCode()
        result = 31 * result + identityPublicKey.contentHashCode()
        result = 31 * result + protocolVersions.hashCode()
        result = 31 * result + capabilities.hashCode()
        result = 31 * result + (relayKey?.contentHashCode() ?: 0)
        result = 31 * result + (operator?.hashCode() ?: 0)
        return result
    }
}

fun SparrowNodeDescriptor.unsigned(): UnsignedNodeDescriptor =
    UnsignedNodeDescriptor(
        nodeId = nodeId,
        clientEndpoint = clientEndpoint,
        federationEndpoint = federationEndpoint,
        mailboxEndpoint = mailboxEndpoint,
        identityPublicKey = identityPublicKey,
        protocolVersions = protocolVersions,
        capabilities = capabilities,
        validUntilEpochMilliseconds = validUntilEpochMilliseconds,
        relayKey = relayKey,
        operator = operator
    )

@Serializable
data class NodeRegistrationRequest(
    val descriptor: SparrowNodeDescriptor
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NodeHeartbeatRequest

        if (timestampEpochMilliseconds != other.timestampEpochMilliseconds) return false
        if (activeConnections != other.activeConnections) return false
        if (nodeId != other.nodeId) return false
        if (nonce != other.nonce) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestampEpochMilliseconds.hashCode()
        result = 31 * result + (activeConnections ?: 0)
        result = 31 * result + nodeId.hashCode()
        result = 31 * result + nonce.hashCode()
        result = 31 * result + signature.contentHashCode()
        return result
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegistryAuthorityCertificate

        if (keyVersion != other.keyVersion) return false
        if (validFromEpochMilliseconds != other.validFromEpochMilliseconds) return false
        if (validUntilEpochMilliseconds != other.validUntilEpochMilliseconds) return false
        if (rootNodeId != other.rootNodeId) return false
        if (!rootPublicKey.contentEquals(other.rootPublicKey)) return false
        if (authorityNodeId != other.authorityNodeId) return false
        if (!authorityPublicKey.contentEquals(other.authorityPublicKey)) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyVersion.hashCode()
        result = 31 * result + validFromEpochMilliseconds.hashCode()
        result = 31 * result + validUntilEpochMilliseconds.hashCode()
        result = 31 * result + rootNodeId.hashCode()
        result = 31 * result + rootPublicKey.contentHashCode()
        result = 31 * result + authorityNodeId.hashCode()
        result = 31 * result + authorityPublicKey.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        return result
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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UnsignedRegistryAuthorityCertificate

        if (keyVersion != other.keyVersion) return false
        if (validFromEpochMilliseconds != other.validFromEpochMilliseconds) return false
        if (validUntilEpochMilliseconds != other.validUntilEpochMilliseconds) return false
        if (rootNodeId != other.rootNodeId) return false
        if (!rootPublicKey.contentEquals(other.rootPublicKey)) return false
        if (authorityNodeId != other.authorityNodeId) return false
        if (!authorityPublicKey.contentEquals(other.authorityPublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyVersion.hashCode()
        result = 31 * result + validFromEpochMilliseconds.hashCode()
        result = 31 * result + validUntilEpochMilliseconds.hashCode()
        result = 31 * result + rootNodeId.hashCode()
        result = 31 * result + rootPublicKey.contentHashCode()
        result = 31 * result + authorityNodeId.hashCode()
        result = 31 * result + authorityPublicKey.contentHashCode()
        return result
    }
}

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegistrySigningCertificate

        if (keyVersion != other.keyVersion) return false
        if (validFromEpochMilliseconds != other.validFromEpochMilliseconds) return false
        if (validUntilEpochMilliseconds != other.validUntilEpochMilliseconds) return false
        if (authorityNodeId != other.authorityNodeId) return false
        if (!authorityPublicKey.contentEquals(other.authorityPublicKey)) return false
        if (signingNodeId != other.signingNodeId) return false
        if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyVersion.hashCode()
        result = 31 * result + validFromEpochMilliseconds.hashCode()
        result = 31 * result + validUntilEpochMilliseconds.hashCode()
        result = 31 * result + authorityNodeId.hashCode()
        result = 31 * result + authorityPublicKey.contentHashCode()
        result = 31 * result + signingNodeId.hashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        return result
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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UnsignedRegistrySigningCertificate

        if (keyVersion != other.keyVersion) return false
        if (validFromEpochMilliseconds != other.validFromEpochMilliseconds) return false
        if (validUntilEpochMilliseconds != other.validUntilEpochMilliseconds) return false
        if (authorityNodeId != other.authorityNodeId) return false
        if (!authorityPublicKey.contentEquals(other.authorityPublicKey)) return false
        if (signingNodeId != other.signingNodeId) return false
        if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyVersion.hashCode()
        result = 31 * result + validFromEpochMilliseconds.hashCode()
        result = 31 * result + validUntilEpochMilliseconds.hashCode()
        result = 31 * result + authorityNodeId.hashCode()
        result = 31 * result + authorityPublicKey.contentHashCode()
        result = 31 * result + signingNodeId.hashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        return result
    }
}

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

/**
 * [aliasClaimKey] is the presence directory's X25519 public key (raw, 32 bytes). Apps encrypt their phone
 * aliases for it (see [AliasClaim]); it is signed with the directory, so a gateway cannot swap in its own key.
 */
@Serializable
data class NodeDirectory(
    val generatedAtEpochMilliseconds: Long,
    val validUntilEpochMilliseconds: Long,
    val nodes: List<SparrowNodeDescriptor>,
    val aliasClaimKey: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NodeDirectory

        if (generatedAtEpochMilliseconds != other.generatedAtEpochMilliseconds) return false
        if (validUntilEpochMilliseconds != other.validUntilEpochMilliseconds) return false
        if (nodes != other.nodes) return false
        if (!aliasClaimKey.contentEquals(other.aliasClaimKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = generatedAtEpochMilliseconds.hashCode()
        result = 31 * result + validUntilEpochMilliseconds.hashCode()
        result = 31 * result + nodes.hashCode()
        result = 31 * result + (aliasClaimKey?.contentHashCode() ?: 0)
        return result
    }
}

@Serializable
data class SignedNodeDirectory(
    val directory: NodeDirectory,
    val authorityNodeId: String,
    val authorityPublicKey: ByteArray,
    val authorityCertificate: RegistryAuthorityCertificate? = null,
    val signingCertificate: RegistrySigningCertificate? = null,
    val signature: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignedNodeDirectory

        if (directory != other.directory) return false
        if (authorityNodeId != other.authorityNodeId) return false
        if (!authorityPublicKey.contentEquals(other.authorityPublicKey)) return false
        if (authorityCertificate != other.authorityCertificate) return false
        if (signingCertificate != other.signingCertificate) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = directory.hashCode()
        result = 31 * result + authorityNodeId.hashCode()
        result = 31 * result + authorityPublicKey.contentHashCode()
        result = 31 * result + (authorityCertificate?.hashCode() ?: 0)
        result = 31 * result + (signingCertificate?.hashCode() ?: 0)
        result = 31 * result + signature.contentHashCode()
        return result
    }
}
