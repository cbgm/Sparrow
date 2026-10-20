package com.cbgm.sparrow.feature.conversationorchestration.incoming.data.packet

import com.cbgm.sparrow.core.base.messagepart.data.mapper.toMessagePart
import com.cbgm.sparrow.core.base.messagepart.data.model.TextDto
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.IncomingMessageContent
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.packet.DirectMessagePacketReader
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket

internal class DirectMessagePacketReaderImpl : DirectMessagePacketReader {
    override fun read(packet: ChatMessagePacket): IncomingMessageContent =
        IncomingMessageContent(
            text = packet.parts.filterIsInstance<TextDto>().singleOrNull()?.text.orEmpty(),
            attachments = packet.parts.filterNot { part -> part is TextDto }.map { part -> part.toMessagePart() },
            replyToMessageId = packet.replyToMessageId
        )
}
