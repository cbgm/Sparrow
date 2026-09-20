package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class ReceiveGroupLeaveRequestUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        memberContactId: String,
        packet: GroupLeaveRequestPacket,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> = repository.receiveLeaveRequest(memberContactId, packet, context)
}
