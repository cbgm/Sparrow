package com.cbgm.sparrow.feature.identity.domain.usecase.direct

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository

class ReceiveDirectInviteDeclinedUseCase(
    private val repository: DirectIdentityExchangeRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactInviteDeclinedPacket
    ): Result<Unit> = repository.receiveDeclined(context, packet)
}
