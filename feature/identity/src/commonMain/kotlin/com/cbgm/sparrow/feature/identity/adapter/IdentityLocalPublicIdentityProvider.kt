package com.cbgm.sparrow.feature.identity.adapter

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository

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
                        "Local Sparrow identity does not exist"
                    )

            LocalPublicIdentity(
                encryptionPublicKey = identity.encryptionPublicKey.copyOf(),
                signingPublicKey = identity.signingPublicKey.copyOf()
            )
        }
}
