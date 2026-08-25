package com.cbgm.sparrow.feature.chats.domain.usecase.group.incoming

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository

class MarkGroupContactIdentityMutualUseCase(
    private val contactKeyExchangeRepository: ContactKeyExchangeRepository
) {
    suspend operator fun invoke(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        contactKeyExchangeRepository.markMutual(
            contactId = contactId,
            expectedRemoteEncryptionPublicKey = encryptionPublicKey,
            expectedRemoteSigningPublicKey = signingPublicKey
        )
}
