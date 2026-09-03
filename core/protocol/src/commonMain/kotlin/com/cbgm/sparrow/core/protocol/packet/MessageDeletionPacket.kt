package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("message_deletion")
data class MessageDeletionPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val messageId: String,
    val deletedAtEpochMilliseconds: Long
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(messageId.isNotBlank()) { "Deleted message ID must not be blank" }
        require(deletedAtEpochMilliseconds >= 0L) { "Deletion timestamp must not be negative" }
    }
}
