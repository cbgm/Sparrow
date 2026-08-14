package com.cbgm.securechat.server.security

import java.security.MessageDigest
import java.security.PublicKey
import java.util.Base64
import java.util.UUID

data class NodeRequestAuthentication(
    val nodeId: String,
    val timestampEpochMilliseconds: Long,
    val nonce: String,
    val signature: String
)

object NodeRequestHeaders {
    const val NODE_ID = "X-SecureChat-Node-Id"
    const val TIMESTAMP = "X-SecureChat-Timestamp"
    const val NONCE = "X-SecureChat-Nonce"
    const val SIGNATURE = "X-SecureChat-Signature"
}

class NodeRequestSigner(
    private val identity: NodeIdentity,
    private val now: () -> Long = System::currentTimeMillis
) {
    fun sign(
        method: String,
        path: String,
        body: String
    ): NodeRequestAuthentication {
        val timestamp = now()
        val nonce = UUID.randomUUID().toString()
        val content = canonicalRequest(method, path, timestamp, nonce, body)
        return NodeRequestAuthentication(
            nodeId = identity.nodeId,
            timestampEpochMilliseconds = timestamp,
            nonce = nonce,
            signature = Base64.getEncoder().encodeToString(Signatures.sign(content, identity.privateKey))
        )
    }
}

class NodeRequestVerifier(
    private val replayProtection: ReplayProtection = ReplayProtection()
) {
    fun verify(
        authentication: NodeRequestAuthentication,
        method: String,
        path: String,
        body: String,
        publicKey: PublicKey
    ): Boolean {
        if (!replayProtection.accept(
                scope = authentication.nodeId,
                nonce = authentication.nonce,
                timestampEpochMilliseconds = authentication.timestampEpochMilliseconds
            )
        ) {
            return false
        }

        return runCatching {
            Signatures.verify(
                content =
                    canonicalRequest(
                        method,
                        path,
                        authentication.timestampEpochMilliseconds,
                        authentication.nonce,
                        body
                    ),
                signature = Base64.getDecoder().decode(authentication.signature),
                publicKey = publicKey
            )
        }.getOrDefault(false)
    }
}

private fun canonicalRequest(
    method: String,
    path: String,
    timestampEpochMilliseconds: Long,
    nonce: String,
    body: String
): ByteArray {
    val bodyHash =
        MessageDigest
            .getInstance("SHA-256")
            .digest(body.encodeToByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    return "$method\n$path\n$timestampEpochMilliseconds\n$nonce\n$bodyHash".encodeToByteArray()
}
