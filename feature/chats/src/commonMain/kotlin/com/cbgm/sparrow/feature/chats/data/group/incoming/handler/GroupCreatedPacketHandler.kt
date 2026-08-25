package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupCreatedIncomingProcessor

internal class GroupCreatedPacketHandler(
    private val processor: GroupCreatedIncomingProcessor
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupCreatedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> {
        val groupPacket =
            packet as? GroupCreatedPacket
                ?: return Result.failure(
                    IllegalArgumentException("GroupCreatedPacketHandler received an incompatible packet")
                )
        return processor.process(context, groupPacket)
    }
}
