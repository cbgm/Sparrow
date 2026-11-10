package com.cbgm.sparrow.protocol.packet

import com.cbgm.sparrow.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typing or voice recording, for a direct chat or, with [groupId], a group. It is encrypted and padded like a message
 * and goes into the contact's mailbox, so nodes cannot tell it apart from one by its content or size; it is never
 * stored, and one older than a few seconds is ignored.
 */
@Serializable
@SerialName("indicator")
data class IndicatorPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val indicatorType: String,
    val groupId: String? = null,
    val sentAtEpochMilliseconds: Long
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(indicatorType.isNotBlank()) { "Indicator type must not be blank" }
        require(groupId == null || groupId.isNotBlank()) { "Group ID must not be blank" }
        require(sentAtEpochMilliseconds >= 0L) { "Indicator timestamp must not be negative" }
    }
}
