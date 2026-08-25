package com.cbgm.sparrow.feature.contacts.adapter

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleIdentityAcknowledgementPacketUseCase

class IdentityAcknowledgementPacketHandler(
    private val handleIdentityAcknowledgementPacket: HandleIdentityAcknowledgementPacketUseCase
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is IdentityAcknowledgementPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        handleIdentityAcknowledgementPacket(
            context = context,
            packet =
                packet as? IdentityAcknowledgementPacket
                    ?: error("IdentityAcknowledgementPacketHandler received an incompatible packet")
        )
}
