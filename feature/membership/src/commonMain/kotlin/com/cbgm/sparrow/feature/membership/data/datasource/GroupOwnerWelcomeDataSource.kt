package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPeerDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.security.GroupSecurityManager

/** Persists owner epoch and queues signed welcomes after a verified join request. */
internal class GroupOwnerWelcomeDataSource(
    private val store: GroupMembershipStoreDataSource,
    private val securityStore: GroupSecurityStoreDataSource,
    private val epoch: GroupEpochDataSource,
    private val initialSecurity: GroupSecurityManager,
    private val epochSecurity: GroupEpochSecurityDataSource,
    private val broadcaster: GroupPacketBroadcaster,
    private val lock: GroupMembershipLock,
    private val localIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider
) {
    suspend fun sendWelcome(
        sourceId: String,
        title: String,
        groupCreatedAtEpochMilliseconds: Long,
        memberEncryptionPublicKey: ByteArray,
        memberSigningPublicKey: ByteArray,
        updatedAtEpochMilliseconds: Long
    ) = lock.withLock {
        val handshake = requireNotNull(store.findBySourceInvitationId(sourceId)) {
            "Group membership handshake was not found"
        }
        check(handshake.perspective == GroupMembershipPerspective.OWNER.name) {
            "Only the group owner can send a welcome"
        }
        if (handshake.status == GroupMembershipStatus.ACTIVE.name ||
            handshake.status == GroupMembershipStatus.WELCOME_SENT.name
        ) {
            return@withLock
        }
        check(handshake.status == GroupMembershipStatus.IDENTITY_READY.name) {
            "Group join identity was not confirmed"
        }

        val peer = GroupMembershipPeerDto(
            id = handshake.contactId,
            displayName = null,
            preferredPhoneNumber = null,
            phoneNumbers = emptyList(),
            encryptionPublicKey = memberEncryptionPublicKey.copyOf(),
            signingPublicKey = memberSigningPublicKey.copyOf(),
            hasMutualIdentity = true
        )
        val currentState = securityStore.findState(handshake.groupId)
        securityStore.findCurrentRemoteMemberKey(handshake.groupId, peer.id)?.let { storedKey ->
            check(
                storedKey.encryptionPublicKey.contentEquals(memberEncryptionPublicKey) &&
                    storedKey.signingPublicKey.contentEquals(memberSigningPublicKey)
            ) {
                "Accepted group member identity does not match the installed epoch"
            }
        }
        val current = epoch.loadCurrentParticipantContacts(handshake.groupId)
            .filterNot { it.id == peer.id }
        val participants = (current + peer).sortedBy { it.id }
        val identity = localIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigning = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val phone = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val payloads = epoch.createMemberPayloads(handshake.groupId, identity, phone, participants)
        val recipients = epoch.createRecipients(handshake.groupId, participants)
        val secured = if (currentState == null) {
            initialSecurity.createOwnedGroup(
                groupId = handshake.groupId,
                title = title,
                createdAtEpochMilliseconds = groupCreatedAtEpochMilliseconds,
                memberPayloads = payloads,
                memberKeys = epoch.createMemberKeys(handshake.groupId, 1, participants),
                recipients = recipients,
                localSigningKeyPair = localSigning
            ).getOrThrow()
        } else if (current.size == 0 && currentState.currentEpoch == 1 &&
            securityStore.findCurrentRemoteMemberKey(handshake.groupId, peer.id) != null
        ) {
            // Recovery when the first epoch was committed before its welcome could be enqueued.
            initialSecurity.createOwnedGroup(
                groupId = handshake.groupId,
                title = title,
                createdAtEpochMilliseconds = groupCreatedAtEpochMilliseconds,
                memberPayloads = payloads,
                memberKeys = epoch.createMemberKeys(handshake.groupId, 1, participants),
                recipients = recipients,
                localSigningKeyPair = localSigning
            ).getOrThrow()
        } else {
            epochSecurity.rotateOwnedGroup(
                groupId = handshake.groupId,
                title = title,
                createdAtEpochMilliseconds = groupCreatedAtEpochMilliseconds,
                updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
                memberPayloads = payloads,
                memberKeys = epoch.createMemberKeys(handshake.groupId, currentState.currentEpoch + 1, participants),
                recipients = recipients,
                localSigningKeyPair = localSigning
            ).getOrThrow()
        }
        broadcaster.enqueueAll(secured.welcomePacketsByContactId).getOrThrow()
        check(
            store.updateStatus(
                membershipId = handshake.membershipId,
                expectedStatus = GroupMembershipStatus.IDENTITY_READY.name,
                newStatus = GroupMembershipStateMachine.transition(
                    handshake.status,
                    GroupMembershipEvent.WELCOME_SENT
                ).name,
                updatedAt = maxOf(handshake.createdAtEpochMilliseconds, updatedAtEpochMilliseconds)
            ) == 1
        ) { "Group membership changed before welcome was queued" }
    }
}
