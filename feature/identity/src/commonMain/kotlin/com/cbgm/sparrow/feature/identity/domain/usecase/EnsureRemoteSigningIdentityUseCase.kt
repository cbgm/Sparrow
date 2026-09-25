package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class EnsureRemoteSigningIdentityUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(
        contactId: String,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        repository.ensureRemoteSigningIdentity(
            peerId = contactId,
            signingPublicKey = signingPublicKey
        )
}
