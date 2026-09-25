package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.feature.membership.domain.model.MembershipJoinRequest
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository

class ReceiveGroupMembershipJoinRequestUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        peerId: String,
        packet: GroupJoinRequestPacket
    ): Result<MembershipJoinRequest?> =
        repository.receiveJoinRequest(peerId, packet)
}
