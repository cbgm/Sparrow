package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.packet

import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.IncomingMessageContent
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket

/** Reads the message parts of an incoming direct chat packet. */
interface DirectMessagePacketReader {
    fun read(packet: ChatMessagePacket): IncomingMessageContent
}
