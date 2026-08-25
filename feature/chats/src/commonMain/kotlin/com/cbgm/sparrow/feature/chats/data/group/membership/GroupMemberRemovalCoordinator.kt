package com.cbgm.sparrow.feature.chats.data.group.membership

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationDirection
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupPacketBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationCoordinator
import com.cbgm.sparrow.feature.contacts.domain.model.Contact

@Suppress("LongParameterList")
internal class GroupMemberRemovalCoordinator(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val groupSecurityManager: GroupSecurityManager,
    private val groupVerificationCoordinator: GroupVerificationCoordinator,
    private val membershipLock: GroupMembershipLock,
    private val identity: GroupMembershipIdentity,
    private val epochCoordinator: GroupEpochCoordinator,
    private val packetBroadcaster: GroupPacketBroadcaster
) {
    suspend fun removeMember(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        removeWithReason(
            groupId = groupId,
            contactId = contactId,
            reason = GroupMemberRemovedPacket.REASON_REMOVED_BY_OWNER
        )

    suspend fun removeDepartingMember(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        removeWithReason(
            groupId = groupId,
            contactId = contactId,
            reason = GroupMemberRemovedPacket.REASON_MEMBER_LEFT
        )

    suspend fun receiveLeaveRequest(
        memberContactId: String,
        packet: GroupLeaveRequestPacket
    ): Result<Unit> =
        runCatching {
            membershipLock.withLock {
                val currentEpoch =
                    groupSecurityManager.findOwnedGroupEpoch(packet.groupId).getOrThrow()
                        ?: error("Active group security state was not found")
                check(packet.epoch <= currentEpoch) {
                    "Group leave request references a future group epoch"
                }
                val memberKey = requireCurrentMemberKey(packet.groupId, memberContactId)
                check(memberKey.signingPublicKey.contentEquals(packet.memberSigningPublicKey)) {
                    "Group leave request signing identity does not match the member"
                }
                membershipPacketProtocol
                    .verifyLeaveRequest(
                        packet = packet,
                        expectedMemberSigningPublicKey = memberKey.signingPublicKey
                    ).getOrThrow()

                removeMemberLocked(
                    groupId = packet.groupId,
                    contactId = memberContactId,
                    reason = GroupMemberRemovedPacket.REASON_MEMBER_LEFT
                )
            }
        }

    private suspend fun removeWithReason(
        groupId: String,
        contactId: String,
        reason: String
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            membershipLock.withLock {
                removeMemberLocked(groupId, contactId, reason)
            }
        }

    private suspend fun removeMemberLocked(
        groupId: String,
        contactId: String,
        reason: String
    ) {
        val removal = loadMemberRemoval(groupId, contactId)
        val removalEpoch = rotateForRemovalIfNeeded(groupId, contactId, reason, removal)
        sendMemberRemovalPacket(groupId, contactId, reason, removalEpoch, removal)
        markOutgoingInvitationRemoved(removal.invitation, removal.removedAt)
        persistMemberRemoval(groupId, contactId, reason, removalEpoch, removal)
        groupVerificationCoordinator.onOwnedMembershipChanged(groupId).getOrThrow()
        chatDao.updateConversationTimestamp(groupId, removal.removedAt)
    }

    private suspend fun loadMemberRemoval(
        groupId: String,
        contactId: String
    ): MemberRemoval {
        groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
        val currentMemberKey = currentMemberKey(groupId, contactId)
        val invitation =
            groupInvitationDao.findByGroupContactAndDirection(
                groupId = groupId,
                contactId = contactId,
                direction = GroupInvitationDirection.OUTGOING.name
            )
        check(currentMemberKey != null || invitation != null) { "Group member was not found" }
        check(invitation?.status?.isTerminalStatus() != true) { "Group member is already inactive" }

        val contact = identity.requireContact(contactId)
        val signingPublicKey =
            currentMemberKey?.signingPublicKey?.copyOf()
                ?: contact.sparrowIdentity?.signingPublicKey?.copyOf()
                ?: byteArrayOf()
        val removedAt =
            maxOf(
                invitation?.createdAtEpochMilliseconds ?: 0L,
                SystemClock.nowEpochMilliseconds()
            )
        return MemberRemoval(
            currentMemberKey = currentMemberKey,
            invitation = invitation,
            contact = contact,
            signingPublicKey = signingPublicKey,
            removedAt = removedAt,
            referenceId = invitation?.invitationId ?: "member-$contactId"
        )
    }

    private suspend fun rotateForRemovalIfNeeded(
        groupId: String,
        contactId: String,
        reason: String,
        removal: MemberRemoval
    ): Int {
        if (removal.currentMemberKey == null) {
            return GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH
        }
        return rotateAfterRemoval(
            groupId = groupId,
            removedContactId = contactId,
            updatedAtEpochMilliseconds = removal.removedAt,
            membershipChange =
                GroupMembershipChangePayload(
                    reason = reason,
                    memberSigningPublicKey = removal.signingPublicKey.copyOf()
                )
        )
    }

    private suspend fun sendMemberRemovalPacket(
        groupId: String,
        contactId: String,
        reason: String,
        removalEpoch: Int,
        removal: MemberRemoval
    ) {
        val packet =
            membershipPacketProtocol
                .createMemberRemoved(
                    invitationId = removal.referenceId,
                    groupId = groupId,
                    epoch = removalEpoch,
                    reason = reason,
                    challenge = removal.invitation?.challenge ?: byteArrayOf(),
                    removedMemberSigningPublicKey = removal.signingPublicKey.copyOf(),
                    removedAtEpochMilliseconds = removal.removedAt,
                    ownerSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                ).getOrThrow()
        protocolOutbox.enqueue(contactId, packet).getOrThrow()
    }

    private suspend fun markOutgoingInvitationRemoved(
        invitation: GroupInvitationEntity?,
        updatedAt: Long
    ) {
        val row = invitation ?: return
        groupInvitationDao.updateStatus(
            invitationId = row.invitationId,
            expectedStatus = row.status,
            newStatus =
                GroupMembershipStateMachine.transition(
                    row.status,
                    GroupMembershipEvent.REMOVE
                ).name,
            updatedAt = updatedAt
        )
    }

    private suspend fun persistMemberRemoval(
        groupId: String,
        contactId: String,
        reason: String,
        removalEpoch: Int,
        removal: MemberRemoval
    ) {
        chatDao.deleteConversationParticipant(groupId, contactId)
        val message =
            if (reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT) {
                GroupMembershipMessageFactory.memberLeft(
                    conversationId = groupId,
                    epoch = removalEpoch,
                    contactId = contactId,
                    contactName = removal.contact.groupMembershipDisplayName(),
                    createdAtEpochMilliseconds = removal.removedAt,
                    eventId = removal.referenceId
                )
            } else {
                GroupMembershipMessageFactory.memberRemoved(
                    conversationId = groupId,
                    epoch = removalEpoch,
                    contactId = contactId,
                    contactName = removal.contact.groupMembershipDisplayName(),
                    createdAtEpochMilliseconds = removal.removedAt,
                    eventId = removal.referenceId
                )
            }
        chatDao.upsertMessage(message)
    }

    private suspend fun rotateAfterRemoval(
        groupId: String,
        removedContactId: String,
        updatedAtEpochMilliseconds: Long,
        membershipChange: GroupMembershipChangePayload?
    ): Int {
        val conversation = chatDao.findConversationById(groupId) ?: error("Group conversation was not found")
        val currentEpoch =
            groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
                ?: error("Active group security state was not found")
        val remainingContacts =
            epochCoordinator.loadCurrentParticipantContacts(groupId)
                .filterNot { contact -> contact.id == removedContactId }
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val nextEpoch = currentEpoch + 1
        val securedGroup =
            groupSecurityManager
                .rotateOwnedGroup(
                    groupId = groupId,
                    title = requireNotNull(conversation.title),
                    createdAtEpochMilliseconds = conversation.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
                    memberPayloads =
                        epochCoordinator.createMemberPayloads(
                            groupId = groupId,
                            localIdentity = localIdentity,
                            localPhoneNumber = localPhoneNumber,
                            contacts = remainingContacts
                        ),
                    memberKeys = epochCoordinator.createMemberKeys(groupId, nextEpoch, remainingContacts),
                    recipients = epochCoordinator.createRecipients(groupId, remainingContacts),
                    localSigningKeyPair = localSigningKeyPair,
                    membershipChange = membershipChange
                ).getOrThrow()

        packetBroadcaster.enqueueAll(securedGroup.welcomePacketsByContactId).getOrThrow()
        return nextEpoch
    }

    private fun String.isTerminalStatus(): Boolean =
        this == GroupInvitationStatus.DECLINED.name ||
            this == GroupInvitationStatus.REMOVED.name ||
            this == GroupInvitationStatus.EXPIRED.name

    private suspend fun requireCurrentMemberKey(
        groupId: String,
        contactId: String
    ): GroupMemberKeyEntity =
        currentMemberKey(groupId, contactId)
            ?: error("Group member is not part of the current group epoch")

    private suspend fun currentMemberKey(
        groupId: String,
        contactId: String
    ): GroupMemberKeyEntity? =
        groupSecurityManager
            .findRemoteMemberKey(
                groupId = groupId,
                contactId = contactId
            ).getOrThrow()

    private data class MemberRemoval(
        val currentMemberKey: GroupMemberKeyEntity?,
        val invitation: GroupInvitationEntity?,
        val contact: Contact,
        val signingPublicKey: ByteArray,
        val removedAt: Long,
        val referenceId: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MemberRemoval

            if (removedAt != other.removedAt) return false
            if (currentMemberKey != other.currentMemberKey) return false
            if (invitation != other.invitation) return false
            if (contact != other.contact) return false
            if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false
            if (referenceId != other.referenceId) return false

            return true
        }

        override fun hashCode(): Int {
            var result = removedAt.hashCode()
            result = 31 * result + (currentMemberKey?.hashCode() ?: 0)
            result = 31 * result + (invitation?.hashCode() ?: 0)
            result = 31 * result + contact.hashCode()
            result = 31 * result + signingPublicKey.contentHashCode()
            result = 31 * result + referenceId.hashCode()
            return result
        }
    }
}
