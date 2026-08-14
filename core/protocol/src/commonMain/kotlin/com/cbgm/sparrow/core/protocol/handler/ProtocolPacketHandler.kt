package com.cbgm.sparrow.core.protocol.handler

import com.cbgm.sparrow.core.protocol.packet.SparrowPacket

interface ProtocolPacketHandler {
    suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit>
}
