package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.feature.membership.data.model.GroupLocalMembershipEndDto
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberPromotionResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

internal class GroupMembershipAdministrationDataSource(
    private val promotionDataSource: GroupMemberPromotionDataSource,
    private val removalDataSource: GroupMemberRemovalDataSource,
    private val leaveDataSource: GroupLeaveDataSource
) {
    suspend fun removeMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> = removalDataSource.removeMember(groupId, contactId, context)

    suspend fun promoteMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberPromotionResult> = promotionDataSource.promoteMember(groupId, contactId, context)

    suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEndDto> = leaveDataSource.transferAdminAndLeave(groupId, contactId, context)

    suspend fun leaveGroup(
        groupId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEndDto> = leaveDataSource.leaveGroup(groupId, context)

    suspend fun receiveLeaveRequest(
        memberContactId: String,
        packet: GroupLeaveRequestPacket,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> =
        removalDataSource.receiveLeaveRequest(memberContactId, packet, context)
}
