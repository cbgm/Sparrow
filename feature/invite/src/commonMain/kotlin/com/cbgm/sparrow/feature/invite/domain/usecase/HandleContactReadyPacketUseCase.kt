package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.feature.invite.domain.repository.DirectInvitationRepository

class HandleContactReadyPacketUseCase(
    private val identityInvitationRepository: DirectInvitationRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactReadyPacket
    ): Result<Unit> =
        identityInvitationRepository.receiveReady(context, packet)
}
