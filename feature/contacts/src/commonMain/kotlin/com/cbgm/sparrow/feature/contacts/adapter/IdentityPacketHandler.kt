package com.cbgm.sparrow.feature.contacts.adapter

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.IdentityPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleIdentityPacketUseCase

class IdentityPacketHandler(
    private val handleIdentityPacket: HandleIdentityPacketUseCase
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is IdentityPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        handleIdentityPacket(
            context = context,
            packet =
                packet as? IdentityPacket
                    ?: error("IdentityPacketHandler received an incompatible packet")
        )
}
