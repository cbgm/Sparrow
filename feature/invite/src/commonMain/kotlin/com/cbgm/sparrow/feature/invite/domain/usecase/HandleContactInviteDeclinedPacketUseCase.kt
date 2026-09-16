package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.feature.invite.domain.repository.DirectIdentityExchangeRepository

class HandleContactInviteDeclinedPacketUseCase(
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactInviteDeclinedPacket
    ): Result<Unit> =
        directIdentityExchangeRepository.receiveDeclined(context, packet)
}
