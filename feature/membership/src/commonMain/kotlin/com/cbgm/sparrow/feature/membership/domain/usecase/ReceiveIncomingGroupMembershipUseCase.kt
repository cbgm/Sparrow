package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository

class ReceiveIncomingGroupMembershipUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        peerId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long,
        shouldStage: Boolean
    ): Result<Unit> =
        repository.receiveIncomingOffer(
            peerId = peerId,
            packet = packet,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds,
            shouldStage = shouldStage
        )
}
