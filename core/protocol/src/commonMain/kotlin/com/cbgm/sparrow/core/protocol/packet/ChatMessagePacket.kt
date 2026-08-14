package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("chat_message")
data class ChatMessagePacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    /**
     * Stable ID of the logical chat message.
     *
     * It may initially be the same as packetId, but keeping it
     * separate allows a message to be retransmitted in another packet.
     */
    val messageId: String,
    val sentAtEpochMilliseconds: Long,
    val text: String,
    val senderPhoneNumber: String? = null
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

        require(sentAtEpochMilliseconds >= 0L) {
            "Message timestamp must not be negative"
        }

        require(text.isNotBlank()) {
            "Message text must not be blank"
        }

        require(senderPhoneNumber == null || senderPhoneNumber.isNotBlank()) {
            "Sender phone number must not be blank"
        }
    }
}
