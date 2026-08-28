package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupInvitationOwnerIdentity
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMembershipRepository
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.contacts.domain.model.SparrowIdentity
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository

class AcceptGroupInvitationUseCase(
    private val membershipRepository: GroupMembershipRepository,
    private val contactRepository: ContactRepository,
    private val contactKeyExchangeRepository: ContactKeyExchangeRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> =
        runCatching {
            val invitationIdentity =
                membershipRepository
                    .getIncomingInvitationOwnerIdentity(groupId)
                    .getOrThrow()
            acceptOwnerIdentity(invitationIdentity)
            membershipRepository.acceptInvitation(groupId).getOrThrow()
        }

    private suspend fun acceptOwnerIdentity(invitation: GroupInvitationOwnerIdentity) {
        val existingIdentity = requireContactIdentity(invitation.contactId)
        val encryptionPublicKey =
            invitation.encryptionPublicKey
                ?: existingIdentity.encryptionPublicKey
        val signingPublicKey =
            invitation.signingPublicKey
                ?: existingIdentity.signingPublicKey
        val sameIdentity =
            existingIdentity.encryptionPublicKey.contentEquals(encryptionPublicKey) &&
                existingIdentity.signingPublicKey.contentEquals(signingPublicKey)

        if (!sameIdentity) {
            contactKeyExchangeRepository
                .acceptInvitationIdentityForHandshake(
                    contactId = invitation.contactId,
                    remoteEncryptionPublicKey = encryptionPublicKey,
                    remoteSigningPublicKey = signingPublicKey
                ).getOrThrow()
        } else if (existingIdentity.keyExchangeStatus != KeyExchangeStatus.MUTUAL) {
            contactKeyExchangeRepository
                .acceptRemoteIdentityForHandshake(
                    contactId = invitation.contactId,
                    expectedRemoteEncryptionPublicKey = encryptionPublicKey,
                    expectedRemoteSigningPublicKey = signingPublicKey
                ).getOrThrow()
        }

        val acceptedIdentity = requireContactIdentity(invitation.contactId)
        check(acceptedIdentity.encryptionPublicKey.contentEquals(encryptionPublicKey)) {
            "Group owner encryption identity changed while the invitation was accepted"
        }
        check(acceptedIdentity.signingPublicKey.contentEquals(signingPublicKey)) {
            "Group owner signing identity changed while the invitation was accepted"
        }
    }

    private suspend fun requireContactIdentity(contactId: String): SparrowIdentity =
        contactRepository
            .getContact(contactId)
            .getOrThrow()
            ?.sparrowIdentity
            ?: error("Group owner identity was not stored")
}
