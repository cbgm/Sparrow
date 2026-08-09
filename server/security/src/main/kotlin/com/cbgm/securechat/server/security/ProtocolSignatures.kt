package com.cbgm.securechat.server.security

import com.cbgm.securechat.server.protocol.ClientRoute
import com.cbgm.securechat.server.protocol.NodeDirectory
import com.cbgm.securechat.server.protocol.NodeHeartbeatRequest
import com.cbgm.securechat.server.protocol.RegistryAuthorityCertificate
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.protocol.SignedNodeDirectory
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.protocol.unsigned

object ProtocolSignatures {
    fun signDescriptor(
        descriptor: SecureChatNodeDescriptor,
        identity: NodeIdentity
    ): SecureChatNodeDescriptor {
        val canonicalDescriptor =
            descriptor.copy(
                nodeId = identity.nodeId,
                identityPublicKey = identity.encodedPublicKey,
                activeConnections = null,
                signature = byteArrayOf()
            )
        return canonicalDescriptor.copy(
            nodeId = identity.nodeId,
            identityPublicKey = identity.encodedPublicKey,
            signature = Signatures.sign(descriptorContent(canonicalDescriptor), identity.privateKey)
        )
    }

    fun verifyDescriptor(descriptor: SecureChatNodeDescriptor): Boolean {
        if (NodeIds.fromPublicKey(descriptor.identityPublicKey) != descriptor.nodeId) {
            return false
        }

        return runCatching {
            Signatures.verify(
                content = descriptorContent(descriptor),
                signature = descriptor.signature,
                publicKey = Signatures.decodePublicKey(descriptor.identityPublicKey)
            )
        }.getOrDefault(false)
    }

    fun verifyHeartbeat(
        heartbeat: NodeHeartbeatRequest,
        descriptor: SecureChatNodeDescriptor
    ): Boolean =
        runCatching {
            Signatures.verify(
                content = serverJson.encodeToString(heartbeat.unsigned()).encodeToByteArray(),
                signature = heartbeat.signature,
                publicKey = Signatures.decodePublicKey(descriptor.identityPublicKey)
            )
        }.getOrDefault(false)

    fun verifyClientRoute(
        route: ClientRoute,
        clientPublicKey: ByteArray
    ): Boolean =
        runCatching {
            Signatures.verify(
                content = serverJson.encodeToString(route.unsigned()).encodeToByteArray(),
                signature = route.clientSignature,
                publicKey = Signatures.decodePublicKey(clientPublicKey)
            )
        }.getOrDefault(false)

    fun signAuthorityCertificate(
        rootIdentity: NodeIdentity,
        authorityIdentity: NodeIdentity,
        keyVersion: Long,
        validFromEpochMilliseconds: Long,
        validUntilEpochMilliseconds: Long
    ): RegistryAuthorityCertificate {
        val unsigned =
            RegistryAuthorityCertificate(
                rootNodeId = rootIdentity.nodeId,
                rootPublicKey = rootIdentity.encodedPublicKey,
                authorityNodeId = authorityIdentity.nodeId,
                authorityPublicKey = authorityIdentity.encodedPublicKey,
                keyVersion = keyVersion,
                validFromEpochMilliseconds = validFromEpochMilliseconds,
                validUntilEpochMilliseconds = validUntilEpochMilliseconds,
                signature = byteArrayOf()
            )
        return unsigned.copy(
            signature =
                Signatures.sign(
                    serverJson.encodeToString(unsigned.unsigned()).encodeToByteArray(),
                    rootIdentity.privateKey
                )
        )
    }

    fun verifyAuthorityCertificate(certificate: RegistryAuthorityCertificate): Boolean {
        if (NodeIds.fromPublicKey(certificate.rootPublicKey) != certificate.rootNodeId) {
            return false
        }
        if (NodeIds.fromPublicKey(certificate.authorityPublicKey) != certificate.authorityNodeId) {
            return false
        }
        return runCatching {
            Signatures.verify(
                content = serverJson.encodeToString(certificate.unsigned()).encodeToByteArray(),
                signature = certificate.signature,
                publicKey = Signatures.decodePublicKey(certificate.rootPublicKey)
            )
        }.getOrDefault(false)
    }

    fun signDirectory(
        directory: NodeDirectory,
        identity: NodeIdentity,
        certificate: RegistryAuthorityCertificate? = null
    ): SignedNodeDirectory =
        SignedNodeDirectory(
            directory = directory,
            authorityNodeId = identity.nodeId,
            authorityPublicKey = identity.encodedPublicKey,
            authorityCertificate = certificate,
            signature =
                Signatures.sign(
                    serverJson.encodeToString(directory).encodeToByteArray(),
                    identity.privateKey
                )
        )

    fun verifyDirectory(directory: SignedNodeDirectory): Boolean {
        val expectedNodeId = NodeIds.fromPublicKey(directory.authorityPublicKey)
        if (expectedNodeId != directory.authorityNodeId) return false

        val certificate = directory.authorityCertificate
        if (certificate != null && !isCertificateValid(certificate, directory)) {
            return false
        }

        return verifyDirectorySignature(directory)
    }

    private fun isCertificateValid(certificate: RegistryAuthorityCertificate, directory: SignedNodeDirectory): Boolean {
        val isStructureValid = verifyAuthorityCertificate(certificate)
        val isNodeIdMatching = certificate.authorityNodeId == directory.authorityNodeId
        val isKeyMatching = certificate.authorityPublicKey.contentEquals(directory.authorityPublicKey)

        return isStructureValid && isNodeIdMatching && isKeyMatching
    }

    private fun verifyDirectorySignature(directory: SignedNodeDirectory): Boolean =
        runCatching {
            val encodedContent = serverJson.encodeToString(directory.directory).encodeToByteArray()
            val decodedKey = Signatures.decodePublicKey(directory.authorityPublicKey)

            Signatures.verify(
                content = encodedContent,
                signature = directory.signature,
                publicKey = decodedKey
            )
        }.getOrDefault(false)

    private fun descriptorContent(descriptor: SecureChatNodeDescriptor): ByteArray =
        serverJson.encodeToString(descriptor.unsigned()).encodeToByteArray()
}
