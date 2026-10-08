package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.packet.IdentityPacket

class ReceiveManualIdentityUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: IdentityPacket
    ): Result<Boolean> = repository.receiveManualIdentity(context, packet)
}
