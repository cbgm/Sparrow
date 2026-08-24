package com.cbgm.sparrow.feature.identity.adapter

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository

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
                        "Local Sparrow identity does not exist"
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
