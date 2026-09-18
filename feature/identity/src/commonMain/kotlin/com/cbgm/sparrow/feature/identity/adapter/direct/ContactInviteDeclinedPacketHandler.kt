package com.cbgm.sparrow.feature.identity.adapter.direct

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.identity.domain.usecase.direct.ReceiveDirectInviteDeclinedUseCase

class ContactInviteDeclinedPacketHandler(
    private val receiveDeclined: ReceiveDirectInviteDeclinedUseCase
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInviteDeclinedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> = receiveDeclined(context, packet as ContactInviteDeclinedPacket)
}
