package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.feature.membership.domain.model.MembershipSigningProof
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository

class ReceiveGroupMembershipReceiptUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        peerId: String,
        packet: GroupInviteReceivedPacket
    ): Result<MembershipSigningProof?> =
        repository.receiveOfferReceipt(peerId, packet)
}
