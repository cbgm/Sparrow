package com.cbgm.securechat.feature.chats.data.group.membership

import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.feature.chats.data.group.invitation.InvitationIdentityPolicy
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.domain.repository.RemoteIdentityOrigin
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact

internal class GroupMembershipIdentity(
    private val getContact: GetContact,
    private val contactKeyExchangeStore: ContactKeyExchangeStore
) {
    suspend fun requireContact(contactId: String): Contact =
        getContact(contactId).getOrThrow()
            ?: error("Contact was not found: $contactId")

    suspend fun stageIncomingOwnerIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Boolean {
        val existing = requireContact(contactId).secureChatIdentity
        val requiresReplacement =
            InvitationIdentityPolicy.requiresReplacement(
                existing = existing,
                encryptionPublicKey = encryptionPublicKey,
                signingPublicKey = signingPublicKey
            )
        if (!requiresReplacement) return false

        storeRemoteIdentity(
            contactId = contactId,
            encryptionPublicKey = encryptionPublicKey,
            signingPublicKey = signingPublicKey
        )
        return true
    }

    suspend fun requireAcceptedOwnerIdentity(invitation: GroupInvitationEntity) {
        val existingIdentity = requireContact(invitation.contactId).secureChatIdentity
        val stagedEncryptionPublicKey =
            invitation.ownerEncryptionPublicKey
                ?: existingIdentity?.encryptionPublicKey
                ?: error("Group owner encryption identity was not stored")
        val stagedSigningPublicKey =
            invitation.ownerSigningPublicKey
                ?: existingIdentity?.signingPublicKey
                ?: error("Group owner signing identity was not stored")
        val sameIdentity =
            existingIdentity != null &&
                existingIdentity.encryptionPublicKey.contentEquals(stagedEncryptionPublicKey) &&
                existingIdentity.signingPublicKey.contentEquals(stagedSigningPublicKey)

        if (!sameIdentity) {
            contactKeyExchangeStore
                .acceptInvitationIdentityForHandshake(
                    contactId = invitation.contactId,
                    remoteEncryptionPublicKey = stagedEncryptionPublicKey,
                    remoteSigningPublicKey = stagedSigningPublicKey
                ).getOrThrow()
        } else if (existingIdentity.keyExchangeStatus != KeyExchangeStatus.MUTUAL) {
            contactKeyExchangeStore
                .acceptRemoteIdentityForHandshake(
                    contactId = invitation.contactId,
                    expectedRemoteEncryptionPublicKey = stagedEncryptionPublicKey,
                    expectedRemoteSigningPublicKey = stagedSigningPublicKey
                ).getOrThrow()
        }

        val acceptedIdentity =
            requireContact(invitation.contactId).secureChatIdentity
                ?: error("Group owner identity was not stored")
        check(acceptedIdentity.encryptionPublicKey.contentEquals(stagedEncryptionPublicKey)) {
            "Group owner encryption identity changed while the invitation was accepted"
        }
        check(acceptedIdentity.signingPublicKey.contentEquals(stagedSigningPublicKey)) {
            "Group owner signing identity changed while the invitation was accepted"
        }
    }

    suspend fun storeMutualIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ) {
        val existingIdentity = requireContact(contactId).secureChatIdentity
        val sameIdentity =
            existingIdentity != null &&
                existingIdentity.encryptionPublicKey.contentEquals(encryptionPublicKey) &&
                existingIdentity.signingPublicKey.contentEquals(signingPublicKey)

        if (!sameIdentity) {
            contactKeyExchangeStore
                .acceptInvitationIdentityForHandshake(
                    contactId = contactId,
                    remoteEncryptionPublicKey = encryptionPublicKey,
                    remoteSigningPublicKey = signingPublicKey
                ).getOrThrow()
        } else if (existingIdentity.keyExchangeStatus != KeyExchangeStatus.MUTUAL) {
            contactKeyExchangeStore
                .acceptRemoteIdentityForHandshake(
                    contactId = contactId,
                    expectedRemoteEncryptionPublicKey = encryptionPublicKey,
                    expectedRemoteSigningPublicKey = signingPublicKey
                ).getOrThrow()
        }

        contactKeyExchangeStore
            .markMutual(
                contactId = contactId,
                expectedRemoteEncryptionPublicKey = encryptionPublicKey,
                expectedRemoteSigningPublicKey = signingPublicKey
            ).getOrThrow()
    }

    suspend fun ensureSigningIdentityMatches(
        contactId: String,
        signingPublicKey: ByteArray
    ) {
        val existing = requireContact(contactId).secureChatIdentity ?: return
        check(existing.signingPublicKey.contentEquals(signingPublicKey)) {
            "Contact signing identity conflicts with the invitation response"
        }
    }

    private suspend fun storeRemoteIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ) {
        contactKeyExchangeStore
            .storeRemoteIdentity(
                contactId = contactId,
                encryptionPublicKey = encryptionPublicKey,
                signingPublicKey = signingPublicKey,
                origin = RemoteIdentityOrigin.CONTACT_INVITATION
            ).getOrThrow()
    }
}
