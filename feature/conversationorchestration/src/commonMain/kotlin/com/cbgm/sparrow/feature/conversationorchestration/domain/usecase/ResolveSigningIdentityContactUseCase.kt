package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.FindRemoteIdentityPeerIdUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase

/** Resolve a group member against Identity's key index, then Contacts' local contact records. */
class ResolveSigningIdentityContactUseCase(
    private val contacts: ContactRepository,
    private val findIdentityPeerId: FindRemoteIdentityPeerIdUseCase,
    private val getRemoteIdentity: GetRemoteIdentityUseCase
) {
    suspend operator fun invoke(
        signingPublicKey: ByteArray,
        encryptionPublicKey: ByteArray,
        phoneNumber: String?
    ): Result<String> = runCatching {
        val identityPeerId = findIdentityPeerId(signingPublicKey).getOrThrow()
        val byIdentity = identityPeerId?.let { contacts.getContact(it).getOrThrow() }
        val contact = byIdentity ?: contacts.findOrCreateByPhoneNumber(
            phoneNumber?.trim()?.takeIf(String::isNotEmpty)
                ?: error("Group member has no phone number")
        ).getOrThrow()
        getRemoteIdentity(contact.id).getOrThrow()?.let { pinned ->
            check(pinned.encryptionPublicKey.contentEquals(encryptionPublicKey)) {
                "Activated group member conflicts with the pinned contact encryption key"
            }
            check(pinned.signingPublicKey.contentEquals(signingPublicKey)) {
                "Activated group member conflicts with the pinned contact signing key"
            }
        }
        contact.id
    }
}
