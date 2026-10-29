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
import java.nio.ByteBuffer

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

    /**
     * Checks the Register proof. The payload binds the routing ID to this connection, this gateway and the
     * nonce the gateway sent, so a captured signature is useless anywhere else.
     */
    fun verifyGatewaySession(
        session: GatewaySessionBinding,
        clientPublicKey: ByteArray,
        clientSignature: ByteArray
    ): Boolean =
        runCatching {
            Signatures.verify(
                content = gatewaySessionContent(session),
                signature = clientSignature,
                publicKey = Signatures.decodePublicKey(clientPublicKey)
            )
        }.getOrDefault(false)

    /**
     * Every field is length-prefixed so no two different sessions can produce the same bytes, and the
     * domain string keeps the signature from being valid for any other Sparrow signing purpose.
     */
    fun gatewaySessionContent(session: GatewaySessionBinding): ByteArray =
        lengthPrefixed(
            GATEWAY_SESSION_DOMAIN.encodeToByteArray(),
            session.routingId.encodeToByteArray(),
            session.connectionId.encodeToByteArray(),
            session.nodeId.encodeToByteArray(),
            session.nonce
        )

    /**
     * Checks a push device registration. Only the holder of the routing ID's identity key can bind a push
     * token to it; otherwise anyone could redirect another user's wake-ups and read their pending inbox.
     */
    fun verifyPushDevice(
        device: PushDeviceBinding,
        clientPublicKey: ByteArray,
        clientSignature: ByteArray
    ): Boolean =
        runCatching {
            Signatures.verify(
                content = pushDeviceContent(device),
                signature = clientSignature,
                publicKey = Signatures.decodePublicKey(clientPublicKey)
            )
        }.getOrDefault(false)

    /**
     * Same length-prefixed layout as [gatewaySessionContent] with its own domain string. The issue time is
     * an 8-byte big-endian field so the server can refuse old proofs.
     */
    fun pushDeviceContent(device: PushDeviceBinding): ByteArray =
        lengthPrefixed(
            PUSH_DEVICE_DOMAIN.encodeToByteArray(),
            device.routingId.encodeToByteArray(),
            device.token.encodeToByteArray(),
            device.platform.encodeToByteArray(),
            ByteBuffer.allocate(Long.SIZE_BYTES).putLong(device.issuedAtEpochMilliseconds).array()
        )

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

    private fun lengthPrefixed(vararg fields: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(fields.sumOf { field -> Int.SIZE_BYTES + field.size })
        fields.forEach { field -> buffer.putInt(field.size).put(field) }
        return buffer.array()
    }

    private const val GATEWAY_SESSION_DOMAIN = "sparrow-gateway-session-v1"
    private const val PUSH_DEVICE_DOMAIN = "sparrow-push-device-v1"
}

/**
 * What a gateway session proof is bound to.
 */
class GatewaySessionBinding(
    val routingId: String,
    val connectionId: String,
    val nodeId: String,
    val nonce: ByteArray
)

/**
 * What a push device registration proof is bound to.
 */
class PushDeviceBinding(
    val routingId: String,
    val token: String,
    val platform: String,
    val issuedAtEpochMilliseconds: Long
)
