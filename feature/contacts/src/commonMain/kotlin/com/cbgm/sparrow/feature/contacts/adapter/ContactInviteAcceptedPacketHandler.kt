package com.cbgm.sparrow.feature.contacts.adapter

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleContactInviteAcceptedPacketUseCase

class ContactInviteAcceptedPacketHandler(
    private val handleContactInviteAcceptedPacket: HandleContactInviteAcceptedPacketUseCase
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInviteAcceptedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        handleContactInviteAcceptedPacket(
            context = context,
            packet = packet as? ContactInviteAcceptedPacket ?: error("Incompatible contact acceptance packet")
        )
}
