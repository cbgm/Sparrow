package com.cbgm.sparrow.feature.membership.data.coordinator

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupMembershipDao
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipIdentity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipBroadcastDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipMessageDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipSecurityDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipVerificationDataSource
import com.cbgm.sparrow.feature.membership.data.groupMembershipDisplayName
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus

@Suppress("LongParameterList")
class GroupMemberRemovalCoordinator(
    private val chatDao: ChatDao,
    private val groupMembershipDao: GroupMembershipDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipProtocolDataSource,
    private val groupSecurityManager: GroupMembershipSecurityDataSource,
    private val groupVerificationCoordinator: GroupMembershipVerificationDataSource,
    private val membershipLock: GroupMembershipLock,
    private val identity: GroupMembershipIdentity,
    private val epochCoordinator: GroupEpochCoordinator,
    private val packetBroadcaster: GroupMembershipBroadcastDataSource,
    private val membershipMessageDataSource: GroupMembershipMessageDataSource
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
        markMembershipRemoved(removal.membership, removal.removedAt)
        persistMemberRemoval(groupId, contactId, reason, removalEpoch, removal)
        groupVerificationCoordinator.onOwnedMembershipChanged(groupId).getOrThrow()
        chatDao.updateConversationTimestamp(groupId, removal.removedAt)
    }

    private suspend fun loadMemberRemoval(
        groupId: String,
        contactId: String
    ): MemberRemovalDto {
        groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
        val currentMemberKey = currentMemberKey(groupId, contactId)
        val membership =
            groupMembershipDao.findByGroupContactAndPerspective(
                groupId = groupId,
                contactId = contactId,
                perspective = GroupMembershipPerspective.OWNER.name
            )
        check(currentMemberKey != null || membership != null) { "Group member was not found" }
        check(membership?.status?.isTerminalStatus() != true) { "Group member is already inactive" }

        val contact = identity.requireContact(contactId)
        val signingPublicKey =
            currentMemberKey?.signingPublicKey?.copyOf()
                ?: contact.sparrowIdentity?.signingPublicKey?.copyOf()
                ?: byteArrayOf()
        val removedAt =
            maxOf(
                membership?.createdAtEpochMilliseconds ?: 0L,
                SystemClock.nowEpochMilliseconds()
            )
        return MemberRemovalDto(
            currentMemberKey = currentMemberKey,
            membership = membership,
            contact = contact,
            signingPublicKey = signingPublicKey,
            removedAt = removedAt,
            referenceId = membership?.sourceInvitationId ?: "member-$contactId"
        )
    }

    private suspend fun rotateForRemovalIfNeeded(
        groupId: String,
        contactId: String,
        reason: String,
        removal: MemberRemovalDto
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
        removal: MemberRemovalDto
    ) {
        val packet =
            membershipPacketProtocol
                .createMemberRemoved(
                    invitationId = removal.referenceId,
                    groupId = groupId,
                    epoch = removalEpoch,
                    reason = reason,
                    challenge = removal.membership?.challenge ?: byteArrayOf(),
                    removedMemberSigningPublicKey = removal.signingPublicKey.copyOf(),
                    removedAtEpochMilliseconds = removal.removedAt,
                    ownerSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                ).getOrThrow()
        protocolOutbox.enqueue(contactId, packet).getOrThrow()
    }

    private suspend fun markMembershipRemoved(
        membership: GroupMembershipEntity?,
        updatedAt: Long
    ) {
        val row = membership ?: return
        groupMembershipDao.updateStatus(
            membershipId = row.membershipId,
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
        removal: MemberRemovalDto
    ) {
        chatDao.deleteConversationParticipant(groupId, contactId)
        val message =
            if (reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT) {
                membershipMessageDataSource.memberLeft(
                    conversationId = groupId,
                    epoch = removalEpoch,
                    contactId = contactId,
                    contactName = removal.contact.groupMembershipDisplayName(),
                    createdAtEpochMilliseconds = removal.removedAt,
                    eventId = removal.referenceId
                )
            } else {
                membershipMessageDataSource.memberRemoved(
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
        this == GroupMembershipStatus.REMOVED.name ||
            this == GroupMembershipStatus.GROUP_DELETED.name ||
            this == GroupMembershipStatus.FAILED.name

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

    private data class MemberRemovalDto(
        val currentMemberKey: GroupMemberKeyEntity?,
        val membership: GroupMembershipEntity?,
        val contact: Contact,
        val signingPublicKey: ByteArray,
        val removedAt: Long,
        val referenceId: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MemberRemovalDto

            if (removedAt != other.removedAt) return false
            if (currentMemberKey != other.currentMemberKey) return false
            if (membership != other.membership) return false
            if (contact != other.contact) return false
            if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false
            if (referenceId != other.referenceId) return false

            return true
        }

        override fun hashCode(): Int {
            var result = removedAt.hashCode()
            result = 31 * result + (currentMemberKey?.hashCode() ?: 0)
            result = 31 * result + (membership?.hashCode() ?: 0)
            result = 31 * result + contact.hashCode()
            result = 31 * result + signingPublicKey.contentHashCode()
            result = 31 * result + referenceId.hashCode()
            return result
        }
    }
}
