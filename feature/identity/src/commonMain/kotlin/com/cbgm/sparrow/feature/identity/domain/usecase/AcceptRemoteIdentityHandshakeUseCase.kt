package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class AcceptRemoteIdentityHandshakeUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        repository.acceptRemoteIdentity(
            peerId = contactId,
            encryptionPublicKey = encryptionPublicKey,
            signingPublicKey = signingPublicKey
        )
}
