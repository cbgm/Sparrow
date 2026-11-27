package com.cbgm.sparrow.feature.transport.data.directory

import kotlinx.serialization.Serializable

@Serializable
internal enum class NodeCapabilityDto {
    GATEWAY,
    MAILBOX
}

@Serializable
internal data class SparrowNodeDescriptorDto(
    val nodeId: String,
    val clientEndpoint: String,
    val mailboxEndpoint: String,
    val identityPublicKey: ByteArray,
    val protocolVersions: Set<Int>,
    val capabilities: Set<NodeCapabilityDto>,
    val validUntilEpochMilliseconds: Long,
    /** Raw X25519 key the node relays onion layers with; null for nodes that do not relay. */
    val relayKey: ByteArray? = null,
    /** Who runs the node, as its operator declares it; null when not declared. */
    val operator: String? = null,
    val signature: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SparrowNodeDescriptorDto

        if (validUntilEpochMilliseconds != other.validUntilEpochMilliseconds) return false
        if (nodeId != other.nodeId) return false
        if (clientEndpoint != other.clientEndpoint) return false
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
        result = 31 * result + nodeId.hashCode()
        result = 31 * result + clientEndpoint.hashCode()
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
internal data class UnsignedNodeDescriptorDto(
    val nodeId: String,
    val clientEndpoint: String,
    val mailboxEndpoint: String,
    val identityPublicKey: ByteArray,
    val protocolVersions: Set<Int>,
    val capabilities: Set<NodeCapabilityDto>,
    val validUntilEpochMilliseconds: Long,
    val relayKey: ByteArray? = null,
    val operator: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UnsignedNodeDescriptorDto

        if (validUntilEpochMilliseconds != other.validUntilEpochMilliseconds) return false
        if (nodeId != other.nodeId) return false
        if (clientEndpoint != other.clientEndpoint) return false
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
        result = 31 * result + mailboxEndpoint.hashCode()
        result = 31 * result + identityPublicKey.contentHashCode()
        result = 31 * result + protocolVersions.hashCode()
        result = 31 * result + capabilities.hashCode()
        result = 31 * result + (relayKey?.contentHashCode() ?: 0)
        result = 31 * result + (operator?.hashCode() ?: 0)
        return result
    }
}

internal fun SparrowNodeDescriptorDto.unsigned(): UnsignedNodeDescriptorDto =
    UnsignedNodeDescriptorDto(
        nodeId = nodeId,
        clientEndpoint = clientEndpoint,
        mailboxEndpoint = mailboxEndpoint,
        identityPublicKey = identityPublicKey,
        protocolVersions = protocolVersions,
        capabilities = capabilities,
        validUntilEpochMilliseconds = validUntilEpochMilliseconds,
        relayKey = relayKey,
        operator = operator
    )

@Serializable
internal data class RegistryAuthorityCertificateDto(
    val rootNodeId: String,
    val rootPublicKey: ByteArray,
    val authorityNodeId: String,
    val authorityPublicKey: ByteArray,
    val keyVersion: Long,
    val validFromEpochMilliseconds: Long,
    val validUntilEpochMilliseconds: Long,
    val signature: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegistryAuthorityCertificateDto

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
internal data class UnsignedRegistryAuthorityCertificateDto(
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

        other as UnsignedRegistryAuthorityCertificateDto

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

internal fun RegistryAuthorityCertificateDto.unsigned(): UnsignedRegistryAuthorityCertificateDto =
    UnsignedRegistryAuthorityCertificateDto(
        rootNodeId = rootNodeId,
        rootPublicKey = rootPublicKey,
        authorityNodeId = authorityNodeId,
        authorityPublicKey = authorityPublicKey,
        keyVersion = keyVersion,
        validFromEpochMilliseconds = validFromEpochMilliseconds,
        validUntilEpochMilliseconds = validUntilEpochMilliseconds
    )

@Serializable
internal data class RegistrySigningCertificateDto(
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

        other as RegistrySigningCertificateDto

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
internal data class UnsignedRegistrySigningCertificateDto(
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

        other as UnsignedRegistrySigningCertificateDto

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

internal fun RegistrySigningCertificateDto.unsigned(): UnsignedRegistrySigningCertificateDto =
    UnsignedRegistrySigningCertificateDto(
        authorityNodeId = authorityNodeId,
        authorityPublicKey = authorityPublicKey,
        signingNodeId = signingNodeId,
        signingPublicKey = signingPublicKey,
        keyVersion = keyVersion,
        validFromEpochMilliseconds = validFromEpochMilliseconds,
        validUntilEpochMilliseconds = validUntilEpochMilliseconds
    )

/** [aliasClaimKey] is the presence directory's key that alias claims are sealed for, signed with the directory. */
@Serializable
internal data class NodeDirectoryDto(
    val generatedAtEpochMilliseconds: Long,
    val validUntilEpochMilliseconds: Long,
    val nodes: List<SparrowNodeDescriptorDto>,
    val aliasClaimKey: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NodeDirectoryDto

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
internal data class SignedNodeDirectoryDto(
    val directory: NodeDirectoryDto,
    val authorityNodeId: String,
    val authorityPublicKey: ByteArray,
    val authorityCertificate: RegistryAuthorityCertificateDto? = null,
    val signingCertificate: RegistrySigningCertificateDto? = null,
    val signature: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignedNodeDirectoryDto

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
