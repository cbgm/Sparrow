package com.cbgm.sparrow.feature.identity.domain.usecase.direct

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository

class ReceiveDirectReadyUseCase(
    private val repository: DirectIdentityExchangeRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactReadyPacket
    ): Result<Unit> = repository.receiveReady(context, packet)
}
