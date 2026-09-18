package com.cbgm.sparrow.feature.identity.domain.usecase.direct

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository

class ReceiveDirectAuthorizationRevokedUseCase(
    private val repository: DirectIdentityExchangeRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: DirectChatAuthorizationRevokedPacket
    ): Result<Unit> = repository.receiveDirectChatAuthorizationRevoked(context, packet)
}
