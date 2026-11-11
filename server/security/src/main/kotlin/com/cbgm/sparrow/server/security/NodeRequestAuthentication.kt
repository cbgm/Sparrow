package com.cbgm.sparrow.server.security

import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

data class NodeRequestAuthentication(
    val nodeId: String,
    val timestampEpochMilliseconds: Long,
    val nonce: String,
    val signature: String
)

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
