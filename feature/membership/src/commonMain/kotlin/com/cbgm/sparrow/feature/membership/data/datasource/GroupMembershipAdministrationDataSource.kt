package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.feature.membership.data.model.GroupLeaveRequirementDto

class GroupMembershipAdministrationDataSource(
    private val promotionDataSource: GroupMemberPromotionDataSource,
    private val removalDataSource: GroupMemberRemovalDataSource,
    private val leaveDataSource: GroupLeaveDataSource
) {
    suspend fun removeMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = removalDataSource.removeMember(groupId, contactId)

    suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirementDto> =
        leaveDataSource.getLeaveRequirement(groupId)

    suspend fun promoteMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = promotionDataSource.promoteMember(groupId, contactId)

    suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String
    ): Result<Unit> = leaveDataSource.transferAdminAndLeave(groupId, contactId)

    suspend fun leaveGroup(groupId: String): Result<Unit> =
        leaveDataSource.leaveGroup(groupId)

    suspend fun receiveLeaveRequest(
        memberContactId: String,
        packet: GroupLeaveRequestPacket
    ): Result<Unit> = removalDataSource.receiveLeaveRequest(memberContactId, packet)

    suspend fun removeDepartingMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = removalDataSource.removeDepartingMember(groupId, contactId)
}
