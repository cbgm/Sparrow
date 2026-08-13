package com.cbgm.securechat.feature.chats.data.model

import com.cbgm.securechat.core.protocol.packet.SecureChatPacket

data class DecodedIncomingPacket(
    val contactId: String,
    val packet: SecureChatPacket,
    val encodedTransportPayload: String,
    val transportMode: String,
    val receivedAtEpochMilliseconds: Long
)
