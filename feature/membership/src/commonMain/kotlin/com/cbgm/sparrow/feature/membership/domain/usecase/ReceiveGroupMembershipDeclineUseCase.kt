package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.feature.membership.domain.model.MembershipDeclineResult
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository

class ReceiveGroupMembershipDeclineUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        peerId: String,
        packet: GroupInviteDeclinedPacket
    ): Result<MembershipDeclineResult?> =
        repository.receiveDecline(peerId, packet)
}
