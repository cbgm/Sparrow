package com.cbgm.sparrow.feature.identity.adapter.direct

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.ReceiveDirectAuthorizationRevokedUseCase

class DirectChatAuthorizationRevokedPacketHandler(
    private val receiveRevoked: ReceiveDirectAuthorizationRevokedUseCase
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is DirectChatAuthorizationRevokedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> = receiveRevoked(context, packet as DirectChatAuthorizationRevokedPacket)
}
