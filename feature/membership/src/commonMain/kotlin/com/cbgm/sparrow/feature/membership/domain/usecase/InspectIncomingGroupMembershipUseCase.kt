package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.IncomingMembershipOffer
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository
import com.cbgm.sparrow.protocol.packet.GroupInvitePacket

class InspectIncomingGroupMembershipUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        peerId: String,
        packet: GroupInvitePacket
    ): Result<IncomingMembershipOffer> =
        repository.inspectIncomingOffer(peerId, packet)
}
