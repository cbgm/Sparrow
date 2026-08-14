package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("read_receipt")
data class ReadReceiptPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    /**
     * ChatMessagePacket.messageId that was read.
     */
    val messageId: String,
    val readAtEpochMilliseconds: Long
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) {
            "Packet ID must not be blank"
        }

        require(version > 0) {
            "Protocol version must be positive"
        }

        require(messageId.isNotBlank()) {
            "Message ID must not be blank"
        }

        require(readAtEpochMilliseconds >= 0L) {
            "Read timestamp must not be negative"
        }
    }
}
