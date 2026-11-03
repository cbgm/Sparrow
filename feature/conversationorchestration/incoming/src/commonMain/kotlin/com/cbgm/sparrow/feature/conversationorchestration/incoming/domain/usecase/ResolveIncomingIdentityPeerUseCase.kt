package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.model.identity.IdentityPeerMerge
import com.cbgm.sparrow.feature.contacts.domain.model.identity.IdentityPeerResolution
import com.cbgm.sparrow.feature.contacts.domain.usecase.identity.InspectContactPeerUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.error.RemoteIdentityReplacementRequiredException
import com.cbgm.sparrow.feature.identity.domain.model.hasKeys
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
    ): Result<IdentityPeerResolution> =
        safeSuspendCall(logFailure = false) {
            resolve(
                resolvedPeerId = resolvedPeerId,
                remotePhoneNumber = remotePhoneNumber,
                remoteEncryptionPublicKey = remoteEncryptionPublicKey,
                remoteSigningPublicKey = remoteSigningPublicKey
            )
        }

    private suspend fun resolve(
        resolvedPeerId: String,
        remotePhoneNumber: String?,
        remoteEncryptionPublicKey: ByteArray,
        remoteSigningPublicKey: ByteArray
    ): IdentityPeerResolution {
        val phonePeerId =
            remotePhoneNumber?.let { inspectContactPeer.findEquivalentPhonePeerId(it).getOrThrow() }
        // An orphaned Identity record must not be selected as a nonexistent Contacts peer.
        val identityPeerId = findRemoteIdentityPeerId(remoteSigningPublicKey).getOrThrow()
            ?.takeIf { inspectContactPeer.containsPeer(it).getOrThrow() }
        // Only keys select an existing contact. The phone number is the sender's own claim: it never files
        // the invitation under a contact, nor makes the sender count as known.
        val targetPeerId = identityPeerId ?: resolvedPeerId
        val identities = setOfNotNull(resolvedPeerId, identityPeerId, phonePeerId).mapNotNull { peerId ->
            getRemoteIdentity(peerId).getOrThrow()?.let { identity -> peerId to identity }
        }.toMap()

        // A Sparrow contact with this number but other keys is never taken over as a side effect of the claim,
        // not even a ONE_WAY/unverified one: the user reviews the proposed keys instead. This check precedes
        // any merge, because the caller merges/deletes duplicate Contacts rows before Identity sees the packet.
        setOfNotNull(phonePeerId, targetPeerId).forEach { peerId ->
            if (identities[peerId]?.hasKeys(remoteEncryptionPublicKey, remoteSigningPublicKey) == false) {
                throw RemoteIdentityReplacementRequiredException(peerId)
            }
        }
        val phoneContactPeerId =
            phonePeerId?.takeIf { peerId ->
                peerId != targetPeerId && inspectContactPeer.isLinkablePhoneContact(peerId).getOrThrow()
            }

        suspend fun mayMerge(peerId: String): Boolean =
            inspectContactPeer.canMergeRoutingDuplicate(peerId, remotePhoneNumber).getOrThrow() &&
                (identities[peerId]?.hasKeys(remoteEncryptionPublicKey, remoteSigningPublicKey) != false)

        val canMergeResolved = targetPeerId != resolvedPeerId && mayMerge(resolvedPeerId)
        val merges =
            listOfNotNull(
                IdentityPeerMerge(resolvedPeerId, targetPeerId, moveBootstrapRouting = true).takeIf { canMergeResolved }
            )
        val keysProveKnownContact =
            targetPeerId == identityPeerId && inspectContactPeer.isKnownContact(targetPeerId).getOrThrow()
        return IdentityPeerResolution(
            peerId = targetPeerId,
            wasKnownPeer = keysProveKnownContact || identities[targetPeerId]?.locallyImported == true,
            merges = merges,
            phoneContactPeerId = phoneContactPeerId
        )
    }
}
