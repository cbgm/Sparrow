package com.cbgm.securechat.feature.chats.data.group.outgoing

import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket

internal class GroupPacketBroadcaster(
    private val protocolOutbox: ProtocolOutbox
) {
    suspend fun enqueueAll(packetsByContactId: Map<String, SecureChatPacket>): Result<Unit> =
        runCatching {
            val failures = mutableListOf<String>()
            packetsByContactId.forEach { (contactId, packet) ->
                protocolOutbox.enqueue(contactId, packet)
                    .onFailure { error ->
                        failures += "$contactId: ${error.message ?: error::class.simpleName.orEmpty()}"
                    }
            }
            check(failures.isEmpty()) {
                "Could not queue group packets for ${failures.joinToString()}"
            }
        }
}
