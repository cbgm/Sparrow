package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.feature.invite.domain.repository.DirectIdentityExchangeRepository

class HandleContactReadyPacketUseCase(
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactReadyPacket
    ): Result<Unit> =
        directIdentityExchangeRepository.receiveReady(context, packet)
}
