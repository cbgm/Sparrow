package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.feature.membership.data.model.GroupLeaveRequirementDto

/**
 * Public entry point for group membership lifecycle operations.
 *
 * The facade keeps packet handlers and repositories on one obvious red line while delegating the
 * actual activation, administration and deletion rules to focused data sources.
 */
class GroupMembershipLifecycleDataSource(
    private val activation: GroupMembershipActivationDataSource,
    private val administration: GroupMembershipAdministrationDataSource,
    private val deletion: GroupMembershipDeletionDataSource
) {
    suspend fun removeMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = administration.removeMember(groupId, contactId)

    suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirementDto> =
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

    suspend fun receiveMemberActivationAcknowledgement(
        packet: GroupMemberActivationAcknowledgementPacket,
        acknowledgingContactId: String
    ): Result<Unit> =
        activation.receiveMemberActivationAcknowledgement(packet, acknowledgingContactId)
}
