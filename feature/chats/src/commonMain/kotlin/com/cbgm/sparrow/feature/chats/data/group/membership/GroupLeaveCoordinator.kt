package com.cbgm.sparrow.feature.chats.data.group.membership

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupLocalCleanupDataSource
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationDirection
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupPacketBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_ADMIN_ROLE
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.chats.data.group.security.isGroupAdminRole
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupLeaveRequirement
import com.cbgm.sparrow.feature.contacts.domain.model.Contact

@Suppress("LongParameterList")
internal class GroupLeaveCoordinator(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val groupSecurityManager: GroupSecurityManager,
    private val membershipLock: GroupMembershipLock,
    private val identity: GroupMembershipIdentity,
    private val epochCoordinator: GroupEpochCoordinator,
    private val localCleanupDataSource: GroupLocalCleanupDataSource,
    private val packetBroadcaster: GroupPacketBroadcaster
) {
    suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirement> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            val localRole = groupSecurityManager.findLocalRole(groupId).getOrThrow()
            if (localRole?.isGroupAdminRole() != true) {
                return@runCatching GroupLeaveRequirement.CanLeave
            }

            val participants = epochCoordinator.findCurrentParticipants(groupId)
            GroupMembershipStateMachine.leaveRequirement(
                isLocalAdmin = true,
                currentMemberContactIds =
                    participants.mapTo(mutableSetOf(), ConversationParticipantEntity::contactId),
                currentAdminContactIds =
                    participants
                        .filter { participant -> participant.role.isGroupAdminRole() }
                        .mapTo(mutableSetOf(), ConversationParticipantEntity::contactId)
            )
        }

    suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            membershipLock.withLock {
                check(groupSecurityManager.findLocalRole(groupId).getOrThrow()?.isGroupAdminRole() == true) {
                    "Only a group admin can transfer administration before leaving"
                }
                leaveAsAdmin(groupId, promoteContactId = contactId)
            }
        }

    suspend fun leaveGroup(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            membershipLock.withLock {
                val localRole = groupSecurityManager.findLocalRole(groupId).getOrThrow()
                if (localRole?.isGroupAdminRole() == true) {
                    leaveAsAdmin(groupId, promoteContactId = null)
                } else {
                    leaveAsMember(groupId)
                }
            }
        }

    private suspend fun leaveAsAdmin(
        groupId: String,
        promoteContactId: String?
    ) {
        val participants = epochCoordinator.findCurrentParticipants(groupId)
        if (participants.isEmpty()) {
            check(promoteContactId == null) { "There is no group member to promote" }
            val epoch = groupSecurityManager.findCurrentEpoch(groupId).getOrThrow() ?: 1
            endLocalMembership(
                groupId = groupId,
                referenceId = "local-admin-leave-$groupId",
                epoch = epoch,
                endedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
            return
        }

        val promotedParticipant = resolvePromotedParticipant(groupId, participants, promoteContactId)
        if (promotedParticipant == null) {
            requireAnotherValidAdmin(groupId, participants)
        }
        rotateGroupBeforeAdminLeaves(groupId, participants, promotedParticipant)
    }

    private suspend fun resolvePromotedParticipant(
        groupId: String,
        participants: List<ConversationParticipantEntity>,
        contactId: String?
    ): ConversationParticipantEntity? {
        val participant =
            contactId?.let { targetId ->
                participants.firstOrNull { row -> row.contactId == targetId }
                    ?: error("Only an active group member can be promoted before leaving")
            } ?: return null
        requireCurrentMemberKey(groupId, participant.contactId)
        return participant
    }

    private suspend fun requireAnotherValidAdmin(
        groupId: String,
        participants: List<ConversationParticipantEntity>
    ) {
        val validAdminExists =
            participants.any { participant ->
                val memberKey = currentMemberKey(groupId, participant.contactId) ?: return@any false
                memberKey.role.isGroupAdminRole()
            }
        check(validAdminExists) { "Promote another group admin before leaving" }
    }

    private suspend fun rotateGroupBeforeAdminLeaves(
        groupId: String,
        participants: List<ConversationParticipantEntity>,
        promotedParticipant: ConversationParticipantEntity?
    ) {
        val conversation = chatDao.findConversationById(groupId) ?: error("Group conversation was not found")
        val currentEpoch =
            groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
                ?: error("Active group security state was not found")
        val contacts =
            participants
                .map { participant -> identity.requireContact(participant.contactId) }
                .sortedBy(Contact::id)
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val now = SystemClock.nowEpochMilliseconds()
        val roleOverrides =
            promotedParticipant
                ?.let { participant -> mapOf(participant.contactId to GROUP_ADMIN_ROLE) }
                .orEmpty()

        val securedGroup =
            groupSecurityManager
                .rotateOwnedGroup(
                    groupId = groupId,
                    title = requireNotNull(conversation.title),
                    createdAtEpochMilliseconds = conversation.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = now,
                    memberPayloads =
                        epochCoordinator.createMemberPayloads(
                            groupId = groupId,
                            localIdentity = localIdentity,
                            localPhoneNumber = localPhoneNumber,
                            contacts = contacts,
                            roleOverrides = roleOverrides
                        ).filterNot { member ->
                            member.signingPublicKey.contentEquals(localSigningKeyPair.publicKey)
                        },
                    memberKeys = epochCoordinator.createMemberKeys(groupId, currentEpoch + 1, contacts, roleOverrides),
                    recipients = epochCoordinator.createRecipients(groupId, contacts),
                    localSigningKeyPair = localSigningKeyPair,
                    membershipChange =
                        GroupMembershipChangePayload(
                            reason = GroupMemberRemovedPacket.REASON_MEMBER_LEFT,
                            memberSigningPublicKey = localSigningKeyPair.publicKey.copyOf()
                        )
                ).getOrThrow()
        packetBroadcaster.enqueueAll(securedGroup.welcomePacketsByContactId).getOrThrow()
        endLocalMembership(
            groupId = groupId,
            referenceId = "local-admin-leave-$groupId",
            epoch = currentEpoch + 1,
            endedAtEpochMilliseconds = now
        )
    }

    private suspend fun leaveAsMember(groupId: String) {
        val participants = epochCoordinator.findCurrentParticipants(groupId)
        val adminParticipant =
            participants
                .filter { participant -> participant.role.isGroupAdminRole() }
                .minByOrNull(ConversationParticipantEntity::contactId)
        if (adminParticipant == null) {
            val epoch = groupSecurityManager.findCurrentEpoch(groupId).getOrThrow() ?: 1
            endLocalMembership(
                groupId = groupId,
                referenceId = "local-member-leave-$groupId",
                epoch = epoch,
                endedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
            return
        }

        val invitation =
            groupInvitationDao.findByGroupId(groupId)
                .firstOrNull { row -> row.direction == GroupInvitationDirection.INCOMING.name }
        val epoch =
            groupSecurityManager.findCurrentEpoch(groupId).getOrThrow()
                ?: error("Active group security state was not found")
        val now = SystemClock.nowEpochMilliseconds()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val leaveRequest =
            membershipPacketProtocol
                .createLeaveRequest(
                    invitationId = invitation?.invitationId ?: "member-${localSigningKeyPair.publicKey.contentHashCode()}",
                    groupId = groupId,
                    epoch = epoch,
                    challenge = invitation?.challenge ?: byteArrayOf(),
                    requestedAtEpochMilliseconds = now,
                    memberSigningKeyPair = localSigningKeyPair
                ).getOrThrow()
        protocolOutbox.enqueue(adminParticipant.contactId, leaveRequest).getOrThrow()
        invitation?.let { row ->
            groupInvitationDao.updateStatus(
                invitationId = row.invitationId,
                expectedStatus = row.status,
                newStatus =
                    GroupMembershipStateMachine.transition(
                        row.status,
                        GroupMembershipEvent.LEAVE_REQUESTED
                    ).name,
                updatedAt = now
            )
        }
        endLocalMembership(
            groupId = groupId,
            referenceId = invitation?.invitationId ?: "local-member-leave-$groupId",
            epoch = epoch,
            endedAtEpochMilliseconds = now
        )
    }

    private suspend fun endLocalMembership(
        groupId: String,
        referenceId: String,
        epoch: Int,
        endedAtEpochMilliseconds: Long
    ) {
        localCleanupDataSource.endMembership(
            GroupMembershipMessageFactory.localMembershipLeft(
                conversationId = groupId,
                invitationId = referenceId,
                epoch = epoch,
                createdAtEpochMilliseconds = endedAtEpochMilliseconds
            )
        )
    }

    private suspend fun requireCurrentMemberKey(
        groupId: String,
        contactId: String
    ) =
        currentMemberKey(groupId, contactId)
            ?: error("Group member is not part of the current group epoch")

    private suspend fun currentMemberKey(
        groupId: String,
        contactId: String
    ) =
        groupSecurityManager
            .findRemoteMemberKey(
                groupId = groupId,
                contactId = contactId
            ).getOrThrow()
}
