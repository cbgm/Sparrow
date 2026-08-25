package com.cbgm.sparrow.feature.chats.data.group.membership

import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupLeaveRequirement

internal class GroupMembershipAdministrationCoordinator(
    private val promotionCoordinator: GroupMemberPromotionCoordinator,
    private val removalCoordinator: GroupMemberRemovalCoordinator,
    private val leaveCoordinator: GroupLeaveCoordinator
) {
    suspend fun removeMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = removalCoordinator.removeMember(groupId, contactId)

    suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirement> =
        leaveCoordinator.getLeaveRequirement(groupId)

    suspend fun promoteMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = promotionCoordinator.promoteMember(groupId, contactId)

    suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String
    ): Result<Unit> = leaveCoordinator.transferAdminAndLeave(groupId, contactId)

    suspend fun leaveGroup(groupId: String): Result<Unit> =
        leaveCoordinator.leaveGroup(groupId)

    suspend fun receiveLeaveRequest(
        memberContactId: String,
        packet: GroupLeaveRequestPacket
    ): Result<Unit> = removalCoordinator.receiveLeaveRequest(memberContactId, packet)

    suspend fun removeDepartingMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = removalCoordinator.removeDepartingMember(groupId, contactId)
}
