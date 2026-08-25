package com.cbgm.sparrow.feature.chats.domain.usecase.group.incoming

import com.cbgm.sparrow.feature.chats.domain.model.group.InvitationIdentityPolicy
import com.cbgm.sparrow.feature.contacts.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository

class StageIncomingGroupOwnerIdentityUseCase(
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
                InvitationIdentityPolicy.requiresReplacement(
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
                    origin = RemoteIdentityOrigin.CONTACT_INVITATION
                ).getOrThrow()
            true
        }
}
