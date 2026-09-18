package com.cbgm.sparrow.feature.identity.adapter.direct

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.ReceiveDirectInviteUseCase

class ContactInvitePacketHandler(
    private val receiveInvite: ReceiveDirectInviteUseCase
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInvitePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> = receiveInvite(context, packet as ContactInvitePacket)
}
