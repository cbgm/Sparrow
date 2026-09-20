package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class ReceiveGroupReadyAcknowledgementUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = repository.receiveReadyAcknowledgement(memberContactId, packet, receivedAtEpochMilliseconds)
}
