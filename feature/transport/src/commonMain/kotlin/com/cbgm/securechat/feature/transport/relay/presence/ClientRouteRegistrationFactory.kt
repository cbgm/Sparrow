package com.cbgm.securechat.feature.transport.relay.presence

import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.feature.transport.relay.model.ClientRoute
import com.cbgm.securechat.feature.transport.relay.model.ClientRouteRegistration
import com.cbgm.securechat.feature.transport.relay.model.UnsignedClientRoute
import kotlinx.serialization.json.Json

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
        expiresAtEpochMilliseconds: Long,
        aliases: List<String> = emptyList()
    ): Result<ClientRouteRegistration> =
        runCatching {
            val routeAliases = aliases.takeIf { it.isNotEmpty() }
            val unsignedRoute =
                UnsignedClientRoute(
                    routingId = routingId,
                    nodeId = nodeId,
                    connectionId = connectionId,
                    generation = generation,
                    expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
                    aliases = routeAliases
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
                        aliases = routeAliases,
                        clientSignature = signature
                    ),
                clientSigningPublicKey = signingKeyPair.publicKey
            )
        }
}
