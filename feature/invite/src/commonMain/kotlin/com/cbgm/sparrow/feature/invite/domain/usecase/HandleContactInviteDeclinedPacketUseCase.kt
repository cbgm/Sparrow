package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.feature.invite.domain.repository.DirectInvitationRepository

class HandleContactInviteDeclinedPacketUseCase(
    private val identityInvitationRepository: DirectInvitationRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactInviteDeclinedPacket
    ): Result<Unit> =
        identityInvitationRepository.receiveDeclined(context, packet)
}
