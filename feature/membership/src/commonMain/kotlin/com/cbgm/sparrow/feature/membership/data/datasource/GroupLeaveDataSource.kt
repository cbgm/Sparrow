package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.model.GROUP_ADMIN_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupLocalMembershipEndDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipParticipantDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPeerDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

@Suppress("LongParameterList")
internal class GroupLeaveDataSource(
    private val membershipStore: GroupMembershipStoreDataSource,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val groupEpochSecurity: GroupEpochSecurityDataSource,
    private val securityStore: GroupSecurityStoreDataSource,
    private val membershipLock: GroupMembershipLock,
    private val epochDataSource: GroupEpochDataSource,
    private val packetBroadcaster: GroupPacketBroadcaster
) {
    suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEndDto> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            membershipLock.withLock {
                check(securityStore.findLocalRole(groupId)?.isGroupAdminRole() == true) {
                    "Only a group admin can transfer administration before leaving"
                }
                leaveAsAdmin(groupId, promoteContactId = contactId, context = context)
            }
        }

    suspend fun leaveGroup(
        groupId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEndDto> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            membershipLock.withLock {
                val localRole = securityStore.findLocalRole(groupId)
                if (localRole?.isGroupAdminRole() == true) {
                    leaveAsAdmin(groupId, promoteContactId = null, context = context)
                } else {
                    leaveAsMember(groupId)
                }
            }
        }

    private suspend fun leaveAsAdmin(
        groupId: String,
        promoteContactId: String?,
        context: GroupMembershipContext
    ): GroupLocalMembershipEndDto {
        val participants = epochDataSource.findCurrentParticipants(groupId)
        if (participants.isEmpty()) {
            check(promoteContactId == null) { "There is no group member to promote" }
            val epoch = securityStore.findCurrentEpoch(groupId) ?: 1
            return endLocalMembership(
                groupId = groupId,
                referenceId = "local-admin-leave-$groupId",
                epoch = epoch,
                endedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        }

        val promotedParticipant = resolvePromotedParticipant(groupId, participants, promoteContactId)
        if (promotedParticipant == null) {
            requireAnotherValidAdmin(groupId, participants)
        }
        return rotateGroupBeforeAdminLeaves(groupId, promotedParticipant, context)
    }

    private suspend fun resolvePromotedParticipant(
        groupId: String,
        participants: List<GroupMembershipParticipantDto>,
        contactId: String?
    ): GroupMembershipParticipantDto? {
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
        participants: List<GroupMembershipParticipantDto>
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
        promotedParticipant: GroupMembershipParticipantDto?,
        context: GroupMembershipContext
    ): GroupLocalMembershipEndDto {
        val currentEpoch =
            securityStore.findOwnedGroupEpoch(groupId)
                ?: error("Active group security state was not found")
        val contacts =
            epochDataSource
                .loadCurrentParticipantContacts(groupId)
                .sortedBy(GroupMembershipPeerDto::id)
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val now = maxOf(context.createdAtEpochMilliseconds, SystemClock.nowEpochMilliseconds())
        val roleOverrides =
            promotedParticipant
                ?.let { participant -> mapOf(participant.contactId to GROUP_ADMIN_ROLE) }
                .orEmpty()

        val securedGroup =
            groupEpochSecurity
                .rotateOwnedGroup(
                    groupId = groupId,
                    title = context.title,
                    createdAtEpochMilliseconds = context.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = now,
                    memberPayloads =
                        epochDataSource.createMemberPayloads(
                            groupId = groupId,
                            localIdentity = localIdentity,
                            localPhoneNumber = localPhoneNumber,
                            contacts = contacts,
                            roleOverrides = roleOverrides
                        ).filterNot { member ->
                            member.signingPublicKey.contentEquals(localSigningKeyPair.publicKey)
                        },
                    memberKeys = epochDataSource.createMemberKeys(groupId, currentEpoch + 1, contacts, roleOverrides),
                    recipients = epochDataSource.createRecipients(groupId, contacts),
                    localSigningKeyPair = localSigningKeyPair,
                    membershipChange =
                        GroupMembershipChangePayload(
                            reason = GroupMemberRemovedPacket.REASON_MEMBER_LEFT,
                            memberSigningPublicKey = localSigningKeyPair.publicKey.copyOf()
                        )
                ).getOrThrow()
        packetBroadcaster.enqueueAll(securedGroup.welcomePacketsByContactId).getOrThrow()
        return endLocalMembership(
            groupId = groupId,
            referenceId = "local-admin-leave-$groupId",
            epoch = currentEpoch + 1,
            endedAtEpochMilliseconds = now
        )
    }

    private suspend fun leaveAsMember(groupId: String): GroupLocalMembershipEndDto {
        val participants = epochDataSource.findCurrentParticipants(groupId)
        val adminParticipant =
            participants
                .filter { participant -> participant.role.isGroupAdminRole() }
                .minByOrNull(GroupMembershipParticipantDto::contactId)
        if (adminParticipant == null) {
            val epoch = securityStore.findCurrentEpoch(groupId) ?: 1
            return endLocalMembership(
                groupId = groupId,
                referenceId = "local-member-leave-$groupId",
                epoch = epoch,
                endedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        }

        val membership =
            membershipStore.findByGroupId(groupId)
                .firstOrNull { row -> row.perspective == GroupMembershipPerspective.MEMBER.name }
        val epoch =
            securityStore.findCurrentEpoch(groupId)
                ?: error("Active group security state was not found")
        val now = SystemClock.nowEpochMilliseconds()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val leaveRequest =
            membershipPacketProtocol
                .createLeaveRequest(
                    invitationId = membership?.sourceInvitationId ?: "member-${localSigningKeyPair.publicKey.contentHashCode()}",
                    groupId = groupId,
                    epoch = epoch,
                    challenge = membership?.challenge ?: byteArrayOf(),
                    requestedAtEpochMilliseconds = now,
                    memberSigningKeyPair = localSigningKeyPair
                ).getOrThrow()
        protocolOutbox.enqueue(adminParticipant.contactId, leaveRequest).getOrThrow()
        membership?.let { row ->
            membershipStore.updateStatus(
                membershipId = row.membershipId,
                expectedStatus = row.status,
                newStatus =
                    GroupMembershipStateMachine.transition(
                        row.status,
                        GroupMembershipEvent.LEAVE_REQUESTED
                    ).name,
                updatedAt = now
            )
        }
        return endLocalMembership(
            groupId = groupId,
            referenceId = membership?.sourceInvitationId ?: "local-member-leave-$groupId",
            epoch = epoch,
            endedAtEpochMilliseconds = now
        )
    }

    private suspend fun endLocalMembership(
        groupId: String,
        referenceId: String,
        epoch: Int,
        endedAtEpochMilliseconds: Long
    ): GroupLocalMembershipEndDto {
        securityStore.retireLocalMembership(groupId, endedAtEpochMilliseconds)
        membershipStore.deleteByGroupId(groupId)
        return GroupLocalMembershipEndDto(
            groupId = groupId,
            referenceId = referenceId,
            epoch = epoch,
            endedAtEpochMilliseconds = endedAtEpochMilliseconds
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
        securityStore.findCurrentRemoteMemberKey(groupId, contactId)
}
