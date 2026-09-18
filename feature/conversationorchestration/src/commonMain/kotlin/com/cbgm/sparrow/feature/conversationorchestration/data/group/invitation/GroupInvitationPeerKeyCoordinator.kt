package com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityOrigin

internal class GroupInvitationPeerKeyCoordinator(
    private val contactRepository: ContactRepository,
    private val contactKeyExchangeRepository: ContactKeyExchangeRepository
) {
    suspend fun stageRemotePeerKeys(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Boolean> =
        runCatching {
            val existingIdentity =
                contactRepository
                    .getContact(peerId)
                    .getOrThrow()
                    ?.sparrowIdentity
                    ?: return@runCatching false

            val encryptionKeyMatches =
                existingIdentity.encryptionPublicKey.contentEquals(encryptionPublicKey)
            val signingKeyMatches =
                existingIdentity.signingPublicKey.contentEquals(signingPublicKey)

            if (encryptionKeyMatches && signingKeyMatches) {
                return@runCatching false
            }

            val keysArePinned =
                existingIdentity.keyExchangeStatus == KeyExchangeStatus.MUTUAL ||
                    existingIdentity.verificationStatus == ContactVerificationStatus.VERIFIED

            if (keysArePinned) {
                check(encryptionKeyMatches) {
                    "Peer encryption key conflicts with the remote membership handshake"
                }
                check(signingKeyMatches) {
                    "Peer signing key conflicts with the remote membership handshake"
                }
            }

            contactKeyExchangeRepository
                .storeRemoteIdentity(
                    contactId = peerId,
                    encryptionPublicKey = encryptionPublicKey,
                    signingPublicKey = signingPublicKey,
                    origin = RemoteIdentityOrigin.REMOTE_HANDSHAKE
                ).getOrThrow()
            true
        }

    suspend fun ensureSigningKeyMatches(
        peerId: String,
        signingPublicKey: ByteArray
    ) {
        val existing =
            contactRepository
                .getContact(peerId)
                .getOrThrow()
                ?.sparrowIdentity
                ?.signingPublicKey
                ?: return
        check(existing.contentEquals(signingPublicKey)) {
            "Peer signing key conflicts with the membership handshake"
        }
    }
}
