package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class ReceiveGroupActivationAcknowledgementUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        packet: GroupMemberActivationAcknowledgementPacket,
        acknowledgingContactId: String,
        transportMode: String
    ): Result<Unit> = repository.receiveMemberActivationAcknowledgement(packet, acknowledgingContactId, transportMode)
}
