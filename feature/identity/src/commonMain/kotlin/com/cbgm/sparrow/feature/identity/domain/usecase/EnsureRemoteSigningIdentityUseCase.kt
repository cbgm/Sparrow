package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityHandshakeRepository

class EnsureRemoteSigningIdentityUseCase(
    private val repository: RemoteIdentityHandshakeRepository
) {
    suspend operator fun invoke(
        contactId: String,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        repository.ensureSigningIdentityMatches(
            contactId = contactId,
            signingPublicKey = signingPublicKey
        )
}
