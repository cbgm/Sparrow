package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalReason
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

@Suppress("LongParameterList")
internal class GroupMemberRemovalDataSource(
    private val membershipStore: GroupMembershipStoreDataSource,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val groupSecurityManager: GroupMembershipSecurityDataSource,
    private val verificationDataSource: GroupMembershipVerificationDataSource,
    private val membershipLock: GroupMembershipLock,
    private val epochDataSource: GroupEpochDataSource,
    private val packetBroadcaster: GroupPacketBroadcaster
) {
    suspend fun removeMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> =
        removeWithReason(
            groupId = groupId,
            contactId = contactId,
            reason = GroupMemberRemovedPacket.REASON_REMOVED_BY_OWNER,
            context = context
        )

    suspend fun receiveLeaveRequest(
        memberContactId: String,
        packet: GroupLeaveRequestPacket,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> =
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
                    reason = GroupMemberRemovedPacket.REASON_MEMBER_LEFT,
                    context = context
                )
            }
        }

    private suspend fun removeWithReason(
        groupId: String,
        contactId: String,
        reason: String,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            membershipLock.withLock {
                removeMemberLocked(groupId, contactId, reason, context)
            }
        }

    private suspend fun removeMemberLocked(
        groupId: String,
        contactId: String,
        reason: String,
        context: GroupMembershipContext
    ): GroupMemberRemovalResult {
        val removal = loadMemberRemoval(groupId, contactId)
        val removalEpoch = rotateForRemovalIfNeeded(groupId, contactId, reason, removal, context)
        sendMemberRemovalPacket(groupId, contactId, reason, removalEpoch, removal)
        markMembershipRemoved(removal.membership, removal.removedAt)
        verificationDataSource.onOwnedMembershipChanged(groupId).getOrThrow()
        return GroupMemberRemovalResult(
            groupId = groupId,
            contactId = contactId,
            epoch = removalEpoch,
            eventId = removal.referenceId,
            updatedAtEpochMilliseconds = removal.removedAt,
            reason =
                if (reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT) {
                    GroupMemberRemovalReason.LEFT
                } else {
                    GroupMemberRemovalReason.REMOVED
                }
        )
    }

    private suspend fun loadMemberRemoval(
        groupId: String,
        contactId: String
    ): MemberRemovalDto {
        groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
        val currentMemberKey = currentMemberKey(groupId, contactId)
        val membership =
            membershipStore.findByGroupContactAndPerspective(
                groupId = groupId,
                contactId = contactId,
                perspective = GroupMembershipPerspective.OWNER.name
            )
        check(currentMemberKey != null || membership != null) { "Group member was not found" }
        check(membership?.status?.isTerminalStatus() != true) { "Group member is already inactive" }

        val signingPublicKey = currentMemberKey?.signingPublicKey?.copyOf() ?: byteArrayOf()
        val removedAt =
            maxOf(
                membership?.createdAtEpochMilliseconds ?: 0L,
                SystemClock.nowEpochMilliseconds()
            )
        return MemberRemovalDto(
            currentMemberKey = currentMemberKey,
            membership = membership,
            signingPublicKey = signingPublicKey,
            removedAt = removedAt,
            referenceId = membership?.sourceInvitationId ?: "member-$contactId"
        )
    }

    private suspend fun rotateForRemovalIfNeeded(
        groupId: String,
        contactId: String,
        reason: String,
        removal: MemberRemovalDto,
        context: GroupMembershipContext
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
                ),
            context = context
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
        membershipStore.updateStatus(
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

    private suspend fun rotateAfterRemoval(
        groupId: String,
        removedContactId: String,
        updatedAtEpochMilliseconds: Long,
        membershipChange: GroupMembershipChangePayload?,
        context: GroupMembershipContext
    ): Int {
        val currentEpoch =
            groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
                ?: error("Active group security state was not found")
        val remainingContacts =
            epochDataSource
                .loadCurrentParticipantContacts(groupId)
                .filterNot { contact -> contact.id == removedContactId }
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val nextEpoch = currentEpoch + 1
        val securedGroup =
            groupSecurityManager
                .rotateOwnedGroup(
                    groupId = groupId,
                    title = context.title,
                    createdAtEpochMilliseconds = context.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
                    memberPayloads =
                        epochDataSource.createMemberPayloads(
                            groupId = groupId,
                            localIdentity = localIdentity,
                            localPhoneNumber = localPhoneNumber,
                            contacts = remainingContacts
                        ),
                    memberKeys = epochDataSource.createMemberKeys(groupId, nextEpoch, remainingContacts),
                    recipients = epochDataSource.createRecipients(groupId, remainingContacts),
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

    private class MemberRemovalDto(
        val currentMemberKey: GroupMemberKeyEntity?,
        val membership: GroupMembershipEntity?,
        val signingPublicKey: ByteArray,
        val removedAt: Long,
        val referenceId: String
    )
}
