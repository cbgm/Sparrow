package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.MembershipJoinRequest
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository
import com.cbgm.sparrow.protocol.packet.GroupJoinRequestPacket

class ReceiveGroupMembershipJoinRequestUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        peerId: String,
        packet: GroupJoinRequestPacket
    ): Result<MembershipJoinRequest?> =
        repository.receiveJoinRequest(peerId, packet)
}
