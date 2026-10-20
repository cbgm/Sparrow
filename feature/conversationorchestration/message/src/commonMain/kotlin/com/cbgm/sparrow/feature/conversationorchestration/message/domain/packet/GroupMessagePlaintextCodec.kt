package com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet

import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupMessagePlaintext
import com.cbgm.sparrow.protocol.message.OperationMessage

/** Converts group message content to and from the plaintext that Membership encrypts. */
interface GroupMessagePlaintextCodec {
    fun encodeMessage(
        messageId: String,
        text: String,
        parts: List<MessagePart>,
        replyToMessageId: String?
    ): String

    fun encodeOperation(message: OperationMessage): String

    fun decode(plaintext: String): GroupMessagePlaintext
}
