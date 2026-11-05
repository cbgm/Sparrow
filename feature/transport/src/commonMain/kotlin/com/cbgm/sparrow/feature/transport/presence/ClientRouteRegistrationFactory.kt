package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.core.crypto.claim.AliasClaimSealer
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.feature.transport.gateway.model.AliasClaim
import com.cbgm.sparrow.feature.transport.gateway.model.ClientRoute
import com.cbgm.sparrow.feature.transport.gateway.model.ClientRouteRegistration
import com.cbgm.sparrow.feature.transport.gateway.model.UnsignedAliasClaim
import com.cbgm.sparrow.feature.transport.gateway.model.UnsignedClientRoute
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import kotlinx.serialization.json.Json

/**
 * Signs the route the gateway sees, without phone aliases. The aliases (and the one-time phone verification token)
 * go into an [AliasClaim] sealed for the presence directory, so the gateway cannot read them.
 */
class ClientRouteRegistrationFactory(
    private val signingKeyPairProvider: LocalSigningKeyPairProvider,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val aliasClaimSealer: AliasClaimSealer,
    private val json: Json
) {
    suspend fun create(
        routingId: String,
        nodeId: String,
        connectionId: String,
        generation: Long,
        expiresAtEpochMilliseconds: Long,
        aliasClaim: PendingAliasClaim? = null
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
                clientSigningPublicKey = signingKeyPair.publicKey,
                sealedAliasClaim =
                    aliasClaim?.let { claim ->
                        sealAliasClaim(
                            unsigned =
                                UnsignedAliasClaim(
                                    routingId = routingId,
                                    connectionId = connectionId,
                                    generation = generation,
                                    aliases = claim.aliases,
                                    phoneVerificationToken = claim.phoneVerificationToken
                                ),
                            signingPrivateKey = signingKeyPair.privateKey,
                            presencePublicKey = claim.presencePublicKey
                        )
                    }
            )
        }

    private suspend fun sealAliasClaim(
        unsigned: UnsignedAliasClaim,
        signingPrivateKey: ByteArray,
        presencePublicKey: ByteArray
    ): ByteArray {
        val signature =
            signatureCrypto
                .sign(payload = json.encodeToString(unsigned).encodeToByteArray(), signingPrivateKey = signingPrivateKey)
                .getOrThrow()
        val claim =
            AliasClaim(
                routingId = unsigned.routingId,
                connectionId = unsigned.connectionId,
                generation = unsigned.generation,
                aliases = unsigned.aliases,
                phoneVerificationToken = unsigned.phoneVerificationToken,
                signature = signature
            )
        return aliasClaimSealer.seal(json.encodeToString(claim).encodeToByteArray(), presencePublicKey).getOrThrow()
    }
}

/** Phone aliases to claim with a route, for the presence directory key [presencePublicKey]. */
class PendingAliasClaim(
    val aliases: List<String>,
    val phoneVerificationToken: String?,
    val presencePublicKey: ByteArray
) {
    override fun toString(): String = "PendingAliasClaim(redacted)"
}
