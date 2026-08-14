package com.cbgm.sparrow.server.security

import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.NodeDirectory
import com.cbgm.sparrow.server.protocol.NodeHeartbeatRequest
import com.cbgm.sparrow.server.protocol.RegistryAuthorityCertificate
import com.cbgm.sparrow.server.protocol.RegistrySigningCertificate
import com.cbgm.sparrow.server.protocol.SignedNodeDirectory
import com.cbgm.sparrow.server.protocol.SparrowNodeDescriptor
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.protocol.unsigned

object ProtocolSignatures {
    fun signDescriptor(
        descriptor: SparrowNodeDescriptor,
        identity: NodeIdentity
    ): SparrowNodeDescriptor {
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

    fun verifyDescriptor(descriptor: SparrowNodeDescriptor): Boolean {
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
        descriptor: SparrowNodeDescriptor
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

    fun signDirectory(
        directory: NodeDirectory,
        identity: NodeIdentity,
        certificate: RegistryAuthorityCertificate? = null,
        signingCertificate: RegistrySigningCertificate? = null
    ): SignedNodeDirectory =
        SignedNodeDirectory(
            directory = directory,
            authorityNodeId = identity.nodeId,
            authorityPublicKey = identity.encodedPublicKey,
            authorityCertificate = certificate,
            signingCertificate = signingCertificate,
            signature =
                Signatures.sign(
                    serverJson.encodeToString(directory).encodeToByteArray(),
                    identity.privateKey
                )
        )

    fun verifyDirectory(directory: SignedNodeDirectory): Boolean {
        val expectedNodeId = NodeIds.fromPublicKey(directory.authorityPublicKey)
        if (expectedNodeId != directory.authorityNodeId) return false

        if (!isCertificateChainValid(directory)) {
            return false
        }

        return verifyDirectorySignature(directory)
    }

    private fun isCertificateChainValid(directory: SignedNodeDirectory): Boolean {
        val authorityCertificate = directory.authorityCertificate ?: return directory.signingCertificate == null
        if (!RegistryCertificateSignatures.verifyAuthorityCertificate(authorityCertificate)) {
            return false
        }

        val signingCertificate =
            directory.signingCertificate
                ?: return authorityCertificate.authorityNodeId == directory.authorityNodeId &&
                    authorityCertificate.authorityPublicKey.contentEquals(directory.authorityPublicKey)

        return RegistryCertificateSignatures.verifySigningCertificate(signingCertificate) &&
            signingCertificate.authorityNodeId == authorityCertificate.authorityNodeId &&
            signingCertificate.authorityPublicKey.contentEquals(authorityCertificate.authorityPublicKey) &&
            signingCertificate.validFromEpochMilliseconds >=
            authorityCertificate.validFromEpochMilliseconds &&
            signingCertificate.validUntilEpochMilliseconds <=
            authorityCertificate.validUntilEpochMilliseconds &&
            signingCertificate.signingNodeId == directory.authorityNodeId &&
            signingCertificate.signingPublicKey.contentEquals(directory.authorityPublicKey)
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

    private fun descriptorContent(descriptor: SparrowNodeDescriptor): ByteArray =
        serverJson.encodeToString(descriptor.unsigned()).encodeToByteArray()
}
