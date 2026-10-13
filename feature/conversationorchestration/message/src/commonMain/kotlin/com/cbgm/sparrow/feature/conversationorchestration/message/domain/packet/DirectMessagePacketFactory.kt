package com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket

interface DirectMessagePacketFactory {
    /** A new packet with a fresh packet ID for an already persisted direct message. */
    suspend fun create(
        messageId: String,
        text: String,
        parts: List<MessagePart>,
        replyToMessageId: String?
    ): Result<ChatMessagePacket>
}
