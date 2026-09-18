package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.feature.membership.domain.model.IncomingMembershipOffer
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository

class InspectIncomingGroupMembershipUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        peerId: String,
        packet: GroupInvitePacket
    ): Result<IncomingMembershipOffer> =
        repository.inspectIncomingOffer(peerId, packet)
}
