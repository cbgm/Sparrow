package com.cbgm.sparrow.protocol.packet

import com.cbgm.sparrow.protocol.message.OperationMessage
import com.cbgm.sparrow.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("operation_message")
data class OperationMessagePacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val message: OperationMessage
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
    }
}
