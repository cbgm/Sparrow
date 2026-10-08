package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.packet.IdentityAcknowledgementPacket

class ReceiveIdentityAcknowledgementUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: IdentityAcknowledgementPacket
    ): Result<Boolean> = repository.receiveIdentityAcknowledgement(context, packet)
}
