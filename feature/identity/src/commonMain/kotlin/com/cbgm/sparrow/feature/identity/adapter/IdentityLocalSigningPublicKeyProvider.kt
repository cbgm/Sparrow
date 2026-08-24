package com.cbgm.sparrow.feature.identity.adapter

import com.cbgm.sparrow.core.protocol.identity.LocalSigningPublicKeyProvider
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository

class IdentityLocalSigningPublicKeyProvider(
    private val identityRepository: IdentityRepository
) : LocalSigningPublicKeyProvider {
    override suspend fun getSigningPublicKey(): Result<ByteArray> =
        runCatching {
            val identity =
                identityRepository
                    .getIdentity()
                    .getOrThrow()
                    ?: error(
                        "Local Sparrow identity does not exist"
                    )

            identity.signingPublicKey
                .copyOf()
        }
}
