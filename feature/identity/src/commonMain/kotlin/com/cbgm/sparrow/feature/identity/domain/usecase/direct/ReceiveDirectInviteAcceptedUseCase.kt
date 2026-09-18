package com.cbgm.sparrow.feature.identity.domain.usecase.direct

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository

class ReceiveDirectInviteAcceptedUseCase(
    private val repository: DirectIdentityExchangeRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactInviteAcceptedPacket
    ): Result<Unit> = repository.receiveAccepted(context, packet)
}
