package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayServerMessage
import com.cbgm.sparrow.feature.transport.gateway.model.GatewaySessionProof
import com.cbgm.sparrow.feature.transport.gateway.model.gatewaySessionPayload
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import kotlinx.serialization.InternalSerializationApi

/**
 * Answers a gateway challenge with the identity signing key, the same key that signs presence routes.
 * The challenge must come from the node the client picked from the signed directory; otherwise the
 * WebSocket reached a different server and the session is refused.
 */
@OptIn(InternalSerializationApi::class)
class CreateGatewaySessionProofUseCase(
    private val signingKeyPairProvider: LocalSigningKeyPairProvider,
    private val signatureCrypto: DetachedSignatureCrypto
) {
    suspend operator fun invoke(
        challenge: GatewayServerMessage.Challenge,
        expectedNodeId: String,
        routingId: String,
        connectionId: String
    ): Result<GatewaySessionProof> =
        safeSuspendCall(logFailure = false) {
            check(challenge.nodeId == expectedNodeId) {
                "Gateway challenge came from node ${challenge.nodeId}, expected $expectedNodeId"
            }
            check(challenge.nonce.size == CHALLENGE_NONCE_BYTES) {
                "Gateway challenge nonce has an unexpected length"
            }
            val signingKeyPair = signingKeyPairProvider.getSigningKeyPair().getOrThrow()
            val payload =
                gatewaySessionPayload(
                    routingId = routingId,
                    connectionId = connectionId,
                    nodeId = challenge.nodeId,
                    nonce = challenge.nonce
                )
            GatewaySessionProof(
                clientSigningPublicKey = signingKeyPair.publicKey,
                clientSignature =
                    signatureCrypto
                        .sign(payload = payload, signingPrivateKey = signingKeyPair.privateKey)
                        .getOrThrow()
            )
        }

    private companion object {
        const val CHALLENGE_NONCE_BYTES = 32
    }
}
