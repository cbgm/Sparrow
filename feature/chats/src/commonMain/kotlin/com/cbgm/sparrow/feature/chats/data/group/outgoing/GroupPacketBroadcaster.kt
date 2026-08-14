package com.cbgm.sparrow.feature.chats.data.group.outgoing

import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket

internal class GroupPacketBroadcaster(
    private val protocolOutbox: ProtocolOutbox
) {
    suspend fun enqueueAll(packetsByContactId: Map<String, SparrowPacket>): Result<Unit> =
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
