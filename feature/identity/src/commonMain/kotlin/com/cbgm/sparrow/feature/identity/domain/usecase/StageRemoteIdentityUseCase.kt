package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityHandshakeRepository

class StageRemoteIdentityUseCase(
    private val repository: RemoteIdentityHandshakeRepository
) {
    suspend operator fun invoke(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Boolean> =
        repository.stage(
            contactId = contactId,
            encryptionPublicKey = encryptionPublicKey,
            signingPublicKey = signingPublicKey
        )
}
