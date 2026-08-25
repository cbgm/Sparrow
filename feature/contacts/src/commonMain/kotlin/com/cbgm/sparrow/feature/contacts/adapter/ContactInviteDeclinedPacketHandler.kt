package com.cbgm.sparrow.feature.contacts.adapter

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleContactInviteDeclinedPacketUseCase

class ContactInviteDeclinedPacketHandler(
    private val handleContactInviteDeclinedPacket: HandleContactInviteDeclinedPacketUseCase
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInviteDeclinedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        handleContactInviteDeclinedPacket(
            context = context,
            packet = packet as? ContactInviteDeclinedPacket ?: error("Incompatible contact decline packet")
        )
}
