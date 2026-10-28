package com.cbgm.sparrow.feature.transport.gateway.model

/**
 * Signed answer to a gateway challenge, sent in [GatewayClientMessage.Register].
 */
class GatewaySessionProof(
    val clientSigningPublicKey: ByteArray,
    val clientSignature: ByteArray
)

/**
 * Bytes signed to sign in to a gateway. Must match the server's ProtocolSignatures.gatewaySessionContent
 * byte for byte. Each field is length-prefixed (4-byte big-endian) so different field values can never
 * produce the same bytes, and the domain string keeps the signature useless for any other purpose.
 */
fun gatewaySessionPayload(
    routingId: String,
    connectionId: String,
    nodeId: String,
    nonce: ByteArray
): ByteArray =
    lengthPrefixed(
        GATEWAY_SESSION_DOMAIN.encodeToByteArray(),
        routingId.encodeToByteArray(),
        connectionId.encodeToByteArray(),
        nodeId.encodeToByteArray(),
        nonce
    )

private fun lengthPrefixed(vararg fields: ByteArray): ByteArray {
    val output = ByteArray(fields.sumOf { field -> LENGTH_PREFIX_BYTES + field.size })
    var offset = 0
    fields.forEach { field ->
        for (shift in LENGTH_PREFIX_SHIFTS) {
            output[offset++] = (field.size ushr shift).toByte()
        }
        field.copyInto(output, destinationOffset = offset)
        offset += field.size
    }
    return output
}

private const val GATEWAY_SESSION_DOMAIN = "sparrow-gateway-session-v1"
private const val LENGTH_PREFIX_BYTES = 4
private val LENGTH_PREFIX_SHIFTS = intArrayOf(24, 16, 8, 0)
