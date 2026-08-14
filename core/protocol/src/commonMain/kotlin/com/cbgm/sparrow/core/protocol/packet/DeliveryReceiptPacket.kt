package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("delivery_receipt")
data class DeliveryReceiptPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    /**
     * ID of the ChatMessagePacket.messageId that was successfully
     * stored by the recipient.
     */
    val messageId: String,
    val deliveredAtEpochMilliseconds: Long
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

        require(deliveredAtEpochMilliseconds >= 0L) {
            "Delivery timestamp must not be negative"
        }
    }
}
