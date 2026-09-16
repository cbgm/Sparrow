package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.feature.invite.domain.repository.DirectInvitationRepository

class HandleContactInviteAcceptedPacketUseCase(
    private val identityInvitationRepository: DirectInvitationRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactInviteAcceptedPacket
    ): Result<Unit> =
        identityInvitationRepository.receiveAccepted(context, packet)
}
