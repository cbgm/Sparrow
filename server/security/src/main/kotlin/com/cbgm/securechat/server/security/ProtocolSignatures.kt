package com.cbgm.securechat.server.security

import com.cbgm.securechat.server.protocol.ClientRoute
import com.cbgm.securechat.server.protocol.NodeDirectory
import com.cbgm.securechat.server.protocol.NodeHeartbeatRequest
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.protocol.SignedNodeDirectory
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.protocol.unsigned
import kotlinx.serialization.encodeToString

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

    fun signDirectory(
        directory: NodeDirectory,
        identity: NodeIdentity
    ): SignedNodeDirectory =
        SignedNodeDirectory(
            directory = directory,
            authorityNodeId = identity.nodeId,
            authorityPublicKey = identity.encodedPublicKey,
            signature =
                Signatures.sign(
                    serverJson.encodeToString(directory).encodeToByteArray(),
                    identity.privateKey
                )
        )

    fun verifyDirectory(directory: SignedNodeDirectory): Boolean {
        if (NodeIds.fromPublicKey(directory.authorityPublicKey) != directory.authorityNodeId) {
            return false
        }
        return runCatching {
            Signatures.verify(
                content = serverJson.encodeToString(directory.directory).encodeToByteArray(),
                signature = directory.signature,
                publicKey = Signatures.decodePublicKey(directory.authorityPublicKey)
            )
        }.getOrDefault(false)
    }

    private fun descriptorContent(descriptor: SecureChatNodeDescriptor): ByteArray =
        serverJson.encodeToString(descriptor.unsigned()).encodeToByteArray()
}
