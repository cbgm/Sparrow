package com.cbgm.sparrow.feature.identity.adapter.direct

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.ReceiveDirectReadyUseCase

class ContactReadyPacketHandler(
    private val receiveReady: ReceiveDirectReadyUseCase
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactReadyPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> = receiveReady(context, packet as ContactReadyPacket)
}
