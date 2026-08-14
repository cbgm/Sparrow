package com.cbgm.sparrow.feature.chats.data.group.membership

import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupLeaveRequirement

/**
 * Public entry point for group membership lifecycle operations.
 *
 * The facade keeps packet handlers and repositories on one obvious red line while delegating the
 * actual invitation, activation, administration and deletion rules to focused coordinators.
 */
class GroupMembershipCoordinator internal constructor(
    private val invitations: GroupInvitationCoordinator,
    private val activation: GroupMembershipActivationCoordinator,
    private val administration: GroupMembershipAdministrationCoordinator,
    private val deletion: GroupMembershipDeletionCoordinator
) {
    suspend fun createGroup(
        title: String,
        contactIds: Set<String>
    ): Result<String> = invitations.createGroup(title, contactIds)

    suspend fun addMembers(
        groupId: String,
        contactIds: Set<String>
    ): Result<Unit> = invitations.addMembers(groupId, contactIds)

    suspend fun removeMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = administration.removeMember(groupId, contactId)

    suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirement> =
        administration.getLeaveRequirement(groupId)

    suspend fun promoteMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = administration.promoteMember(groupId, contactId)

    suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String
    ): Result<Unit> = administration.transferAdminAndLeave(groupId, contactId)

    suspend fun leaveGroup(groupId: String): Result<Unit> = administration.leaveGroup(groupId)

    suspend fun deleteGroupConversation(groupId: String): Result<Unit> =
        deletion.deleteGroupConversation(groupId)

    suspend fun receiveLeaveRequest(
        memberContactId: String,
        packet: GroupLeaveRequestPacket
    ): Result<Unit> = administration.receiveLeaveRequest(memberContactId, packet)

    suspend fun receiveInvite(
        ownerContactId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = invitations.receiveInvite(ownerContactId, packet, receivedAtEpochMilliseconds)

    suspend fun acceptInvitation(groupId: String): Result<Unit> =
        invitations.acceptInvitation(groupId)

    suspend fun declineInvitation(groupId: String): Result<Unit> =
        invitations.declineInvitation(groupId)

    suspend fun receiveInviteReceived(
        memberContactId: String,
        packet: GroupInviteReceivedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        invitations.receiveInviteReceived(
            memberContactId = memberContactId,
            packet = packet,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )

    suspend fun receiveJoinRequest(
        memberContactId: String,
        packet: GroupJoinRequestPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        invitations.receiveJoinRequest(memberContactId, packet, receivedAtEpochMilliseconds)

    suspend fun receiveDecline(
        memberContactId: String,
        packet: GroupInviteDeclinedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        invitations.receiveDecline(memberContactId, packet, receivedAtEpochMilliseconds)

    suspend fun receiveReadyAcknowledgement(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        activation.receiveReadyAcknowledgement(
            memberContactId = memberContactId,
            packet = packet,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )

    suspend fun activateGroupIfReady(groupId: String): Result<Unit> =
        activation.activateGroupIfReady(groupId)

    suspend fun receiveMemberActivationAcknowledgement(
        packet: GroupMemberActivationAcknowledgementPacket,
        acknowledgingContactId: String
    ): Result<Unit> =
        activation.receiveMemberActivationAcknowledgement(packet, acknowledgingContactId)
}
