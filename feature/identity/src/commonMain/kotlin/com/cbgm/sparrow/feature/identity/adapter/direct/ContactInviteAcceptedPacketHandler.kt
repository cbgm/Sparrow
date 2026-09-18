package com.cbgm.sparrow.feature.identity.adapter.direct

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.ReceiveDirectInviteAcceptedUseCase

class ContactInviteAcceptedPacketHandler(
    private val receiveAccepted: ReceiveDirectInviteAcceptedUseCase
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInviteAcceptedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> = receiveAccepted(context, packet as ContactInviteAcceptedPacket)
}
