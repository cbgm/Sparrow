package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.IdentityPacket
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class ReceiveManualIdentityUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: IdentityPacket
    ): Result<Boolean> = repository.receiveManualIdentity(context, packet)
}
