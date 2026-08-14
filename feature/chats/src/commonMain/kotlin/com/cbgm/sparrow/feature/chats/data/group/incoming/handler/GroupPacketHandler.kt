package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket

/**
 * Handles exactly one family of incoming group packets.
 *
 * Group handlers are deliberately not registered as global protocol handlers.
 * They are reachable only through [com.cbgm.sparrow.feature.chats.data.group.incoming.GroupIncomingPacketProcessor].
 */
interface GroupPacketHandler {
    fun canHandle(packet: SparrowPacket): Boolean

    suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit>
}
