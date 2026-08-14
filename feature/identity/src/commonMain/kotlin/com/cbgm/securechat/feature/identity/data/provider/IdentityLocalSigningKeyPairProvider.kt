package com.cbgm.securechat.feature.identity.data.provider

import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository

class IdentityLocalSigningKeyPairProvider(
    private val identityRepository: IdentityRepository
) : LocalSigningKeyPairProvider {
    override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> =
        runCatching {
            val identity =
                identityRepository
                    .getIdentity()
                    .getOrThrow()
                    ?: error(
                        "Local SecureChat identity does not exist"
                    )

            val privateKey =
                identityRepository
                    .getSigningPrivateKey()
                    .getOrThrow()

            LocalSigningKeyPair(
                publicKey = identity.signingPublicKey.copyOf(),
                privateKey = privateKey.copyOf()
            )
        }
}
