package com.cbgm.securechat.feature.identity.data.provider

import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentity
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository

class IdentityLocalPublicIdentityProvider(
    private val identityRepository: IdentityRepository
) : LocalPublicIdentityProvider {
    override suspend fun getLocalPublicIdentity(): Result<LocalPublicIdentity> =
        runCatching {
            val identity =
                identityRepository
                    .getIdentity()
                    .getOrThrow()
                    ?: error(
                        "Local SecureChat identity does not exist"
                    )

            LocalPublicIdentity(
                encryptionPublicKey = identity.encryptionPublicKey.copyOf(),
                signingPublicKey = identity.signingPublicKey.copyOf()
            )
        }
}
