package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.feature.transport.gateway.model.ClientRoute
import com.cbgm.sparrow.feature.transport.gateway.model.ClientRouteRegistration
import com.cbgm.sparrow.feature.transport.gateway.model.UnsignedClientRoute
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import kotlinx.serialization.json.Json

/** Signs the route the gateway sees. It names no phone aliases: those go to the Control Planes ([AliasClaimRegistrar]). */
class ClientRouteRegistrationFactory(
    private val signingKeyPairProvider: LocalSigningKeyPairProvider,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val json: Json
) {
    suspend fun create(
        routingId: String,
        nodeId: String,
        connectionId: String,
        generation: Long,
        expiresAtEpochMilliseconds: Long
    ): Result<ClientRouteRegistration> =
        runCatching {
            val unsignedRoute =
                UnsignedClientRoute(
                    routingId = routingId,
                    nodeId = nodeId,
                    connectionId = connectionId,
                    generation = generation,
                    expiresAtEpochMilliseconds = expiresAtEpochMilliseconds
                )
            val signingKeyPair = signingKeyPairProvider.getSigningKeyPair().getOrThrow()
            val payload = json.encodeToString(unsignedRoute).encodeToByteArray()
            val signature =
                signatureCrypto
                    .sign(payload = payload, signingPrivateKey = signingKeyPair.privateKey)
                    .getOrThrow()

            signatureCrypto
                .verify(
                    payload = payload,
                    signingPublicKey = signingKeyPair.publicKey,
                    signature = signature
                ).getOrElse { error ->
                    throw IllegalStateException(
                        "Local signing public/private key pair is inconsistent",
                        error
                    )
                }

            ClientRouteRegistration(
                route =
                    ClientRoute(
                        routingId = routingId,
                        nodeId = nodeId,
                        connectionId = connectionId,
                        generation = generation,
                        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
                        clientSignature = signature
                    ),
                clientSigningPublicKey = signingKeyPair.publicKey
            )
        }
}
