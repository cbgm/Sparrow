package com.cbgm.sparrow.feature.chats.domain.usecase.group.incoming

import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository

class EstablishGroupMemberIdentityUseCase(
    private val contactRepository: ContactRepository,
    private val contactKeyExchangeRepository: ContactKeyExchangeRepository
) {
    suspend operator fun invoke(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        runCatching {
            val existingIdentity =
                contactRepository
                    .getContact(contactId)
                    .getOrThrow()
                    ?.sparrowIdentity
            val sameIdentity =
                existingIdentity != null &&
                    existingIdentity.encryptionPublicKey.contentEquals(encryptionPublicKey) &&
                    existingIdentity.signingPublicKey.contentEquals(signingPublicKey)

            if (!sameIdentity) {
                contactKeyExchangeRepository
                    .acceptInvitationIdentityForHandshake(
                        contactId = contactId,
                        remoteEncryptionPublicKey = encryptionPublicKey,
                        remoteSigningPublicKey = signingPublicKey
                    ).getOrThrow()
            } else if (existingIdentity.keyExchangeStatus != KeyExchangeStatus.MUTUAL) {
                contactKeyExchangeRepository
                    .acceptRemoteIdentityForHandshake(
                        contactId = contactId,
                        expectedRemoteEncryptionPublicKey = encryptionPublicKey,
                        expectedRemoteSigningPublicKey = signingPublicKey
                    ).getOrThrow()
            }

            contactKeyExchangeRepository
                .markMutual(
                    contactId = contactId,
                    expectedRemoteEncryptionPublicKey = encryptionPublicKey,
                    expectedRemoteSigningPublicKey = signingPublicKey
                ).getOrThrow()
        }
}
