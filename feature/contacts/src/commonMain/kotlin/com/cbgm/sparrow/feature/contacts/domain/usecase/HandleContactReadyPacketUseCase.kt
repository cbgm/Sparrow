package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository

class HandleContactReadyPacketUseCase(
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactReadyPacket
    ): Result<Unit> =
        directIdentityExchangeRepository.receiveReady(context, packet)
}
