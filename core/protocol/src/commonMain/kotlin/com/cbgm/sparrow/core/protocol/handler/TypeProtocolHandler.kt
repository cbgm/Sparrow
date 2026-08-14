package com.cbgm.sparrow.core.protocol.handler

import com.cbgm.sparrow.core.protocol.packet.SparrowPacket

/**
 * Handles one or more concrete Sparrow packet types.
 *
 * Implementations belong to feature modules:
 *
 * feature:chats
 * -> ChatMessagePacketHandler
 *
 * feature:contacts
 * -> ContactInvitePacketHandler
 * -> ContactReadyPacketHandler
 */
interface TypedProtocolPacketHandler {
    fun canHandle(packet: SparrowPacket): Boolean

    suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit>
}
