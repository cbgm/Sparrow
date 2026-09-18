package com.cbgm.sparrow.feature.identity.domain.usecase.direct

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository

class ReceiveDirectInviteUseCase(
    private val repository: DirectIdentityExchangeRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactInvitePacket
    ): Result<Unit> = repository.receiveInvite(context, packet)
}
