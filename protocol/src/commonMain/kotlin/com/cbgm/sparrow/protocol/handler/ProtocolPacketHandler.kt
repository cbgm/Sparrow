package com.cbgm.sparrow.protocol.handler

import com.cbgm.sparrow.protocol.packet.SparrowPacket

interface ProtocolPacketHandler {
    suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit>
}
