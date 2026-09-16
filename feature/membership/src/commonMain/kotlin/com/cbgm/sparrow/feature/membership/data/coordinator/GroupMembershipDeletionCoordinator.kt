package com.cbgm.sparrow.feature.membership.data.coordinator

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipBroadcastDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipCleanupDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipSecurityDataSource
import com.cbgm.sparrow.feature.membership.data.model.GROUP_LEFT_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupInvitationDirection
import com.cbgm.sparrow.feature.membership.data.model.GroupInvitationStatus

@Suppress("LongParameterList")
class GroupMembershipDeletionCoordinator(
    private val groupInvitationDao: GroupInvitationDao,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipProtocolDataSource,
    private val groupSecurityManager: GroupMembershipSecurityDataSource,
    private val packetBroadcaster: GroupMembershipBroadcastDataSource,
    private val administration: GroupMembershipAdministrationCoordinator,
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

            val invitations = groupInvitationDao.findByGroupId(groupId)
            val hasOutgoingInvitation =
                invitations.any { invitation ->
                    invitation.direction == GroupInvitationDirection.OUTGOING.name
                }
            if (hasOutgoingInvitation) {
                deleteOwnedGroupConversation(groupId, invitations)
            } else {
                deleteJoinedGroupConversation(groupId, invitations)
            }
        }

    private suspend fun deleteOwnedGroupConversation(
        groupId: String,
        invitations: List<GroupInvitationEntity>
    ) {
        membershipLock.withLock {
            val now =
                maxOf(
                    SystemClock.nowEpochMilliseconds(),
                    invitations.maxOfOrNull(GroupInvitationEntity::createdAtEpochMilliseconds) ?: 0L
                )
            val epoch =
                groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
                    ?: GroupConversationDeletedPacket.PENDING_GROUP_EPOCH
            val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()

            val packetsByContactId =
                invitations
                    .filterNot { invitation -> invitation.status.isTerminalStatus() }
                    .associate { invitation ->
                        invitation.contactId to
                            membershipPacketProtocol
                                .createConversationDeleted(
                                    invitationId = invitation.invitationId,
                                    groupId = groupId,
                                    epoch = epoch,
                                    challenge = invitation.challenge,
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
        invitations: List<GroupInvitationEntity>
    ) {
        val invitation =
            invitations
                .filter { candidate ->
                    candidate.direction == GroupInvitationDirection.INCOMING.name &&
                        (
                            candidate.status.isIncomingStatus() ||
                                candidate.status == GroupInvitationStatus.ACTIVE.name
                        )
                }.maxByOrNull(GroupInvitationEntity::updatedAtEpochMilliseconds)
        if (invitation != null) {
            when (invitation.status) {
                GroupInvitationStatus.ACTIVE.name -> administration.leaveGroup(groupId).getOrThrow()
                GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
                GroupInvitationStatus.JOIN_SENT.name,
                GroupInvitationStatus.WAITING_FOR_ACTIVATION.name -> {
                    val decline =
                        membershipPacketProtocol
                            .createDecline(
                                invitationId = invitation.invitationId,
                                groupId = groupId,
                                challenge = invitation.challenge,
                                memberSigningKeyPair =
                                    localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                            ).getOrThrow()
                    protocolOutbox.enqueue(invitation.contactId, decline).getOrThrow()
                }
            }
        }

        localCleanupDataSource.delete(
            groupId = groupId,
            deletedAtEpochMilliseconds =
                maxOf(
                    SystemClock.nowEpochMilliseconds(),
                    invitations.maxOfOrNull(GroupInvitationEntity::createdAtEpochMilliseconds) ?: 0L
                )
        )
    }

    private fun String.isIncomingStatus(): Boolean =
        this == GroupInvitationStatus.AWAITING_ACCEPTANCE.name ||
            this == GroupInvitationStatus.JOIN_SENT.name ||
            this == GroupInvitationStatus.WAITING_FOR_ACTIVATION.name

    private fun String.isTerminalStatus(): Boolean =
        this == GroupInvitationStatus.DECLINED.name ||
            this == GroupInvitationStatus.REMOVED.name ||
            this == GroupInvitationStatus.EXPIRED.name
}
