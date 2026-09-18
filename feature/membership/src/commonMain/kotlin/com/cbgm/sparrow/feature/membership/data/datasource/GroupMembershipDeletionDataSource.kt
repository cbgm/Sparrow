package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipBroadcastDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipCleanupDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipSecurityDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipStoreDataSource
import com.cbgm.sparrow.feature.membership.data.model.GROUP_LEFT_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus

@Suppress("LongParameterList")
internal class GroupMembershipDeletionDataSource(
    private val membershipStore: GroupMembershipStoreDataSource,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipProtocolDataSource,
    private val groupSecurityManager: GroupMembershipSecurityDataSource,
    private val packetBroadcaster: GroupMembershipBroadcastDataSource,
    private val administration: GroupMembershipAdministrationDataSource,
    private val membershipLock: GroupMembershipLock,
    private val localCleanupDataSource: GroupMembershipCleanupDataSource
) {
    suspend fun deleteGroupConversation(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            val localRole = groupSecurityManager.findLocalRole(groupId).getOrThrow()
            if (localRole != null) {
                if (localRole != GROUP_LEFT_ROLE) {
                    administration.leaveGroup(groupId).getOrThrow()
                }
                localCleanupDataSource.deleteConversationHistory(
                    groupId = groupId,
                    deletedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
                return@runCatching
            }

            val memberships = membershipStore.findByGroupId(groupId)
            val hasOwnerMembership =
                memberships.any { membership ->
                    membership.perspective == GroupMembershipPerspective.OWNER.name
                }
            if (hasOwnerMembership) {
                deleteOwnedGroupConversation(groupId, memberships)
            } else {
                deleteJoinedGroupConversation(groupId, memberships)
            }
        }

    private suspend fun deleteOwnedGroupConversation(
        groupId: String,
        memberships: List<GroupMembershipEntity>
    ) {
        membershipLock.withLock {
            val now =
                maxOf(
                    SystemClock.nowEpochMilliseconds(),
                    memberships.maxOfOrNull(GroupMembershipEntity::createdAtEpochMilliseconds) ?: 0L
                )
            val epoch =
                groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
                    ?: GroupConversationDeletedPacket.PENDING_GROUP_EPOCH
            val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()

            val packetsByContactId =
                memberships
                    .filterNot { membership -> membership.status.isTerminalStatus() }
                    .associate { membership ->
                        membership.contactId to
                            membershipPacketProtocol
                                .createConversationDeleted(
                                    invitationId = membership.sourceInvitationId,
                                    groupId = groupId,
                                    epoch = epoch,
                                    challenge = membership.challenge,
                                    deletedAtEpochMilliseconds = now,
                                    ownerSigningKeyPair = signingKeyPair
                                ).getOrThrow()
                    }
            packetBroadcaster.enqueueAll(packetsByContactId).getOrThrow()

            localCleanupDataSource.delete(groupId, now)
        }
    }

    private suspend fun deleteJoinedGroupConversation(
        groupId: String,
        memberships: List<GroupMembershipEntity>
    ) {
        val membership =
            memberships
                .filter { candidate ->
                    candidate.perspective == GroupMembershipPerspective.MEMBER.name &&
                        (
                            candidate.status.isIncomingStatus() ||
                                candidate.status == GroupMembershipStatus.ACTIVE.name
                        )
                }.maxByOrNull(GroupMembershipEntity::updatedAtEpochMilliseconds)
        if (membership != null) {
            when (membership.status) {
                GroupMembershipStatus.ACTIVE.name -> administration.leaveGroup(groupId).getOrThrow()
                GroupMembershipStatus.STAGED.name,
                GroupMembershipStatus.JOIN_REQUEST_SENT.name,
                GroupMembershipStatus.WAITING_FOR_ACTIVATION.name -> {
                    val decline =
                        membershipPacketProtocol
                            .createDecline(
                                invitationId = membership.sourceInvitationId,
                                groupId = groupId,
                                challenge = membership.challenge,
                                memberSigningKeyPair =
                                    localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                            ).getOrThrow()
                    protocolOutbox.enqueue(membership.contactId, decline).getOrThrow()
                }
            }
        }

        localCleanupDataSource.delete(
            groupId = groupId,
            deletedAtEpochMilliseconds =
                maxOf(
                    SystemClock.nowEpochMilliseconds(),
                    memberships.maxOfOrNull(GroupMembershipEntity::createdAtEpochMilliseconds) ?: 0L
                )
        )
    }

    private fun String.isIncomingStatus(): Boolean =
        this == GroupMembershipStatus.STAGED.name ||
            this == GroupMembershipStatus.JOIN_REQUEST_SENT.name ||
            this == GroupMembershipStatus.WAITING_FOR_ACTIVATION.name

    private fun String.isTerminalStatus(): Boolean =
        this == GroupMembershipStatus.REMOVED.name ||
            this == GroupMembershipStatus.GROUP_DELETED.name ||
            this == GroupMembershipStatus.FAILED.name
}
