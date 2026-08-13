package com.cbgm.securechat.feature.identity.data.provider

import com.cbgm.securechat.core.protocol.identity.LocalSigningPublicKeyProvider
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository

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
                        "Local SecureChat identity does not exist"
                    )

            identity.signingPublicKey
                .copyOf()
        }
}
