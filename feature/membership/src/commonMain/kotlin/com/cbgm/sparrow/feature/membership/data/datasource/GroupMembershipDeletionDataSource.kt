package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.model.GROUP_LEFT_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

@Suppress("LongParameterList")
internal class GroupMembershipDeletionDataSource(
    private val membershipStore: GroupMembershipStoreDataSource,
    private val securityStore: GroupSecurityStoreDataSource,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val packetBroadcaster: GroupPacketBroadcaster,
    private val administration: GroupMembershipAdministrationDataSource,
    private val membershipLock: GroupMembershipLock
) {
    suspend fun deleteGroupConversation(
        groupId: String,
        context: GroupMembershipContext
    ): Result<Long> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            val localRole = securityStore.findLocalRole(groupId)
            if (localRole != null) {
                if (localRole != GROUP_LEFT_ROLE) {
                    administration.leaveGroup(groupId, context).getOrThrow()
                }
                membershipStore.deleteByGroupId(groupId)
                return@runCatching SystemClock.nowEpochMilliseconds()
            }

            val memberships = membershipStore.findByGroupId(groupId)
            val hasOwnerMembership =
                memberships.any { membership ->
                    membership.perspective == GroupMembershipPerspective.OWNER.name
                }
            if (hasOwnerMembership) {
                deleteOwnedGroupConversation(groupId, memberships)
            } else {
                deleteJoinedGroupConversation(groupId, memberships, context)
            }
        }

    private suspend fun deleteOwnedGroupConversation(
        groupId: String,
        memberships: List<GroupMembershipEntity>
    ): Long =
        membershipLock.withLock {
            val now =
                maxOf(
                    SystemClock.nowEpochMilliseconds(),
                    memberships.maxOfOrNull(GroupMembershipEntity::createdAtEpochMilliseconds) ?: 0L
                )
            val epoch =
                securityStore.findOwnedGroupEpoch(groupId)
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

            securityStore.deleteGroup(groupId)
            membershipStore.deleteByGroupId(groupId)
            now
        }

    private suspend fun deleteJoinedGroupConversation(
        groupId: String,
        memberships: List<GroupMembershipEntity>,
        context: GroupMembershipContext
    ): Long {
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
                GroupMembershipStatus.ACTIVE.name -> administration.leaveGroup(groupId, context).getOrThrow()
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

        val deletedAt = maxOf(
            SystemClock.nowEpochMilliseconds(),
            memberships.maxOfOrNull(GroupMembershipEntity::createdAtEpochMilliseconds) ?: 0L
        )
        securityStore.deleteGroup(groupId)
        membershipStore.deleteByGroupId(groupId)
        return deletedAt
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
