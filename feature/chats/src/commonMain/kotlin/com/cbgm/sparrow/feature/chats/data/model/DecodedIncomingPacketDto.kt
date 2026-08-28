package com.cbgm.sparrow.feature.chats.data.model

import com.cbgm.sparrow.core.protocol.packet.SparrowPacket

data class DecodedIncomingPacketDto(
    val contactId: String,
    val packet: SparrowPacket,
    val encodedTransportPayload: String,
    val transportMode: String,
    val receivedAtEpochMilliseconds: Long
)
