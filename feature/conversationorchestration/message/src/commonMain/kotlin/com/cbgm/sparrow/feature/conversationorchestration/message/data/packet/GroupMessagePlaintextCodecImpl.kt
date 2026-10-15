package com.cbgm.sparrow.feature.conversationorchestration.message.data.packet

import com.cbgm.sparrow.core.messagepart.data.mapper.toDto
import com.cbgm.sparrow.core.messagepart.data.mapper.toMessagePart
import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupMessagePlaintext
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.GroupMessagePlaintextCodec
import com.cbgm.sparrow.protocol.message.GroupMessageContent
import com.cbgm.sparrow.protocol.message.GroupMessageContentCodec
import com.cbgm.sparrow.protocol.message.OperationMessage
import com.cbgm.sparrow.protocol.message.OperationMessageCodec

internal class GroupMessagePlaintextCodecImpl(
    private val groupMessageContentCodec: GroupMessageContentCodec,
    private val operationMessageCodec: OperationMessageCodec
) : GroupMessagePlaintextCodec {
    override fun encodeMessage(
        messageId: String,
        text: String,
        parts: List<MessagePart>,
        replyToMessageId: String?
    ): String =
        groupMessageContentCodec.encode(
            GroupMessageContent(
                parts =
                    buildList {
                        text.takeIf(String::isNotBlank)?.let { value -> add(TextDto(id = messageId, text = value)) }
                        addAll(parts.map { it.toDto() })
                    },
                replyToMessageId = replyToMessageId
            )
        )

    override fun encodeOperation(message: OperationMessage): String = operationMessageCodec.encode(message)

    override fun decode(plaintext: String): GroupMessagePlaintext {
        if (operationMessageCodec.canDecode(plaintext)) {
            return GroupMessagePlaintext.Operation(operationMessageCodec.decode(plaintext))
        }
        val content = groupMessageContentCodec.decode(plaintext)
        return GroupMessagePlaintext.Message(
            text = content.parts.filterIsInstance<TextDto>().singleOrNull()?.text.orEmpty(),
            attachments = content.parts.filterNot { part -> part is TextDto }.map { it.toMessagePart() },
            replyToMessageId = content.replyToMessageId
        )
    }
}
