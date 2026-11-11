package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.decoy

import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.protocol.packet.CoverPacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket

/** A contact's decoy ([DecoyEnvelopes]): nothing to do; its envelope is acknowledged like any other. */
class CoverPacketHandler : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is CoverPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> = Result.success(Unit)
}
