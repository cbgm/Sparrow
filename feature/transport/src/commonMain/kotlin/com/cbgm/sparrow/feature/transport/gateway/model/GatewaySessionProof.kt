package com.cbgm.sparrow.feature.transport.gateway.model

import com.cbgm.sparrow.feature.transport.signing.lengthPrefixed

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

private const val GATEWAY_SESSION_DOMAIN = "sparrow-gateway-session-v1"
