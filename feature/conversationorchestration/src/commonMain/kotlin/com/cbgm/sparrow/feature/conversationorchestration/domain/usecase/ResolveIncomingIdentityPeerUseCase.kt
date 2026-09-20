package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.identity.IdentityPeerMerge
import com.cbgm.sparrow.feature.contacts.domain.model.identity.IdentityPeerResolution
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.InspectContactPeerUseCase
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.usecase.FindRemoteIdentityPeerIdUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase

/** Combines Identity-owned key/trust state with Contacts-owned contact matching. */
class ResolveIncomingIdentityPeerUseCase(
    private val inspectContactPeer: InspectContactPeerUseCase,
    private val findRemoteIdentityPeerId: FindRemoteIdentityPeerIdUseCase,
    private val getRemoteIdentity: GetRemoteIdentityUseCase
) {
    suspend operator fun invoke(
        resolvedPeerId: String,
        remotePhoneNumber: String?,
        remoteEncryptionPublicKey: ByteArray,
        remoteSigningPublicKey: ByteArray
    ): IdentityPeerResolution {
        val phonePeerId = remotePhoneNumber?.let { inspectContactPeer.findEquivalentPhonePeerId(it) }
        // An orphaned Identity record must not be selected as a nonexistent Contacts peer.
        val identityPeerId = findRemoteIdentityPeerId(remoteSigningPublicKey).getOrThrow()
            ?.takeIf { inspectContactPeer.containsPeer(it) }
        val targetPeerId = phonePeerId ?: identityPeerId ?: resolvedPeerId
        val identities = setOfNotNull(resolvedPeerId, identityPeerId, targetPeerId).mapNotNull { peerId ->
            getRemoteIdentity(peerId).getOrThrow()?.let { identity -> peerId to identity }
        }.toMap()

        if (phonePeerId == null && identityPeerId != null) {
            val stored = identities[targetPeerId]
            if (stored != null && !stored.matches(remoteEncryptionPublicKey, remoteSigningPublicKey)) {
                check(
                    stored.keyExchangeStatus != KeyExchangeStatus.MUTUAL &&
                        stored.verificationStatus != ContactVerificationStatus.VERIFIED
                ) { "Contact identity changed; reset the contact before accepting new keys" }
            }
        }

        suspend fun mayMerge(peerId: String): Boolean =
            inspectContactPeer.canMergeRoutingDuplicate(peerId, remotePhoneNumber) &&
                (identities[peerId]?.matches(remoteEncryptionPublicKey, remoteSigningPublicKey) != false)

        val canMergeResolved = targetPeerId != resolvedPeerId && mayMerge(resolvedPeerId)
        val canMergeIdentity = identityPeerId != null && identityPeerId != targetPeerId &&
            identityPeerId != resolvedPeerId && mayMerge(identityPeerId)
        val merges = buildList {
            if (canMergeResolved) {
                add(IdentityPeerMerge(resolvedPeerId, targetPeerId, moveBootstrapRouting = true))
            }
            if (canMergeIdentity) {
                add(IdentityPeerMerge(requireNotNull(identityPeerId), targetPeerId, moveBootstrapRouting = false))
            }
        }
        return IdentityPeerResolution(
            peerId = targetPeerId,
            wasKnownPeer = inspectContactPeer.isKnownContact(targetPeerId) ||
                identities[targetPeerId]?.locallyImported == true,
            merges = merges
        )
    }
}

private fun RemotePeerIdentity.matches(encryption: ByteArray, signing: ByteArray): Boolean =
    encryptionPublicKey.contentEquals(encryption) && signingPublicKey.contentEquals(signing)
