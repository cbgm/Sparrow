package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.membership.domain.model.GroupOwnerIdentityReplacementPolicy

class StageGroupOwnerIdentityUseCase(
    private val contactRepository: ContactRepository,
    private val contactKeyExchangeRepository: ContactKeyExchangeRepository
) {
    suspend operator fun invoke(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Boolean> =
        runCatching {
            val existingIdentity =
                contactRepository
                    .getContact(contactId)
                    .getOrThrow()
                    ?.sparrowIdentity

            val requiresReplacement =
                GroupOwnerIdentityReplacementPolicy.requiresReplacement(
                    existing = existingIdentity,
                    encryptionPublicKey = encryptionPublicKey,
                    signingPublicKey = signingPublicKey
                )
            if (!requiresReplacement) return@runCatching false

            contactKeyExchangeRepository
                .storeRemoteIdentity(
                    contactId = contactId,
                    encryptionPublicKey = encryptionPublicKey,
                    signingPublicKey = signingPublicKey,
                    origin = RemoteIdentityOrigin.REMOTE_HANDSHAKE
                ).getOrThrow()
            true
        }
}
