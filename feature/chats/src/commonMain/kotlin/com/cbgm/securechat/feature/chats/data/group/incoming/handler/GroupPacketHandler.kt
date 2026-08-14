package com.cbgm.securechat.feature.chats.data.group.incoming.handler

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket

/**
 * Handles exactly one family of incoming group packets.
 *
 * Group handlers are deliberately not registered as global protocol handlers.
 * They are reachable only through [com.cbgm.securechat.feature.chats.data.group.incoming.GroupIncomingPacketProcessor].
 */
interface GroupPacketHandler {
    fun canHandle(packet: SecureChatPacket): Boolean

    suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit>
}
