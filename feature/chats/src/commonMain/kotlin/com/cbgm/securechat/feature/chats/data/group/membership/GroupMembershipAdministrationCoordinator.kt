package com.cbgm.securechat.feature.chats.data.group.membership

import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.feature.chats.data.group.invitation.GroupInvitationDirection
import com.cbgm.securechat.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.group.invitation.resolveInvitationUpdatedAt
import com.cbgm.securechat.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.securechat.feature.chats.data.group.outgoing.GroupPacketBroadcaster
import com.cbgm.securechat.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.securechat.feature.chats.data.group.security.GROUP_ADMIN_ROLE
import com.cbgm.securechat.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.securechat.feature.chats.data.group.security.isGroupAdminRole
import com.cbgm.securechat.feature.chats.data.group.storage.GroupLocalDataCleaner
import com.cbgm.securechat.feature.chats.data.group.verification.GroupVerificationCoordinator
import com.cbgm.securechat.feature.chats.domain.model.group.GroupLeaveRequirement
import com.cbgm.securechat.feature.contacts.domain.model.Contact

@Suppress("LongParameterList")
internal class GroupMembershipAdministrationCoordinator(
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
    private val localDataCleaner: GroupLocalDataCleaner,
    private val packetBroadcaster: GroupPacketBroadcaster
) {
    suspend fun removeMember(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }

            membershipLock.withLock {
                removeMemberLocked(
                    groupId = groupId,
                    contactId = contactId,
                    reason = GroupMemberRemovedPacket.REASON_REMOVED_BY_OWNER
                )
            }
        }

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

    suspend fun promoteMember(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            membershipLock.withLock {
                promoteMemberLocked(groupId, contactId)
            }
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
                val memberKey =
                    requireCurrentMemberKey(packet.groupId, memberContactId)
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

    private suspend fun promoteMemberLocked(
        groupId: String,
        contactId: String
    ) {
        val conversation = chatDao.findConversationById(groupId) ?: error("Group conversation was not found")
        val currentEpoch =
            groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
                ?: error("Active group security state was not found")
        val participants = epochCoordinator.findCurrentParticipants(groupId)
        val target =
            participants.firstOrNull { participant -> participant.contactId == contactId }
                ?: error("Only an active group member can be promoted")
        if (target.role.isGroupAdminRole()) {
            return
        }
        val contacts = participants.map { participant -> identity.requireContact(participant.contactId) }.sortedBy(Contact::id)
        requireCurrentMemberKey(groupId, contactId)
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val nextEpoch = currentEpoch + 1
        val roleOverrides = mapOf(contactId to GROUP_ADMIN_ROLE)
        val securedGroup =
            groupSecurityManager
                .rotateOwnedGroup(
                    groupId = groupId,
                    title = requireNotNull(conversation.title),
                    createdAtEpochMilliseconds = conversation.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                    memberPayloads =
                        epochCoordinator.createMemberPayloads(
                            groupId = groupId,
                            localIdentity = localIdentity,
                            localPhoneNumber = localPhoneNumber,
                            contacts = contacts,
                            roleOverrides = roleOverrides
                        ),
                    memberKeys = epochCoordinator.createMemberKeys(groupId, nextEpoch, contacts, roleOverrides),
                    recipients = epochCoordinator.createRecipients(groupId, contacts),
                    localSigningKeyPair = localSigningKeyPair
                ).getOrThrow()
        packetBroadcaster.enqueueAll(securedGroup.welcomePacketsByContactId).getOrThrow()
        check(chatDao.updateConversationParticipantRole(groupId, contactId, GROUP_ADMIN_ROLE) == 1) {
            "Promoted group member disappeared while the new epoch was created"
        }
        groupVerificationCoordinator.onOwnedMembershipChanged(groupId).getOrThrow()
        chatDao.updateConversationTimestamp(groupId, SystemClock.nowEpochMilliseconds())
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
        val contacts = participants.map { participant -> identity.requireContact(participant.contactId) }.sortedBy(Contact::id)
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
                newStatus = GroupMembershipStateMachine.transition(row.status, GroupMembershipEvent.LEAVE_REQUESTED).name,
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
        localDataCleaner.endMembership(
            GroupMembershipMessageFactory.localMembershipLeft(
                conversationId = groupId,
                invitationId = referenceId,
                epoch = epoch,
                createdAtEpochMilliseconds = endedAtEpochMilliseconds
            )
        )
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
                ?: contact.secureChatIdentity?.signingPublicKey?.copyOf()
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

    suspend fun removeDepartingMember(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            membershipLock.withLock {
                removeMemberLocked(
                    groupId = groupId,
                    contactId = contactId,
                    reason = GroupMemberRemovedPacket.REASON_MEMBER_LEFT
                )
            }
        }

    private fun String.isTerminalStatus(): Boolean =
        this == GroupInvitationStatus.DECLINED.name ||
            this == GroupInvitationStatus.REMOVED.name ||
            this == GroupInvitationStatus.EXPIRED.name

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

    private data class MemberRemoval(
        val currentMemberKey: GroupMemberKeyEntity?,
        val invitation: GroupInvitationEntity?,
        val contact: Contact,
        val signingPublicKey: ByteArray,
        val removedAt: Long,
        val referenceId: String
    )
}
