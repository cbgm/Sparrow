package com.cbgm.sparrow.server.gateway

import com.cbgm.sparrow.server.protocol.GatewayClientMessage
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.GatewaySessionBinding
import com.cbgm.sparrow.server.security.ProtocolSignatures

/**
 * Decides whether a Register proves ownership of its routing ID. Without this check anyone could
 * register as any routing ID and receive and delete that user's pending envelopes.
 */
internal object GatewaySessionValidator {
    fun validationFailure(
        message: GatewayClientMessage.Register,
        nonce: ByteArray?,
        expectedNodeId: String
    ): GatewaySessionValidationFailure? =
        when {
            nonce == null -> GatewaySessionValidationFailure.CHALLENGE_USED

            ClientRoutingIds.isBootstrapRoutingId(message.routingId) ->
                GatewaySessionValidationFailure.BOOTSTRAP_ROUTING_ID

            !ClientRoutingIds.matchesSigningPublicKey(
                message.routingId,
                message.clientSigningPublicKey
            ) -> GatewaySessionValidationFailure.ROUTING_IDENTITY

            !ProtocolSignatures.verifyGatewaySession(
                session =
                    GatewaySessionBinding(
                        routingId = message.routingId,
                        connectionId = message.connectionId,
                        nodeId = expectedNodeId,
                        nonce = nonce
                    ),
                clientPublicKey = message.clientSigningPublicKey,
                clientSignature = message.clientSignature
            ) -> GatewaySessionValidationFailure.SIGNATURE

            else -> null
        }
}

internal enum class GatewaySessionValidationFailure {
    CHALLENGE_USED,
    BOOTSTRAP_ROUTING_ID,
    ROUTING_IDENTITY,
    SIGNATURE
}
