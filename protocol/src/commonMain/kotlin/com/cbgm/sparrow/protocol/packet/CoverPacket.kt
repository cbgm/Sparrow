package com.cbgm.sparrow.protocol.packet

import com.cbgm.sparrow.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A decoy: sent next to real packets so a mailbox node cannot tell from timing which mailboxes a send was meant for.
 * It is encrypted and padded like a message, so no node can tell it apart; the recipient drops it.
 */
@Serializable
@SerialName("cover")
data class CoverPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
    }
}
