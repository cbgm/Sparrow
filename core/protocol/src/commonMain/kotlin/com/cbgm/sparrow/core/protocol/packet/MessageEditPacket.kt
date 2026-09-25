package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("message_edit")
data class MessageEditPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val messageId: String,
    val editedAtEpochMilliseconds: Long,
    val text: String
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(messageId.isNotBlank()) { "Edited message ID must not be blank" }
        require(editedAtEpochMilliseconds >= 0L) { "Edit timestamp must not be negative" }
        require(text.isNotBlank()) { "Edited message text must not be blank" }
    }
}
