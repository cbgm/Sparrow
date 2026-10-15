package com.cbgm.sparrow.feature.conversationorchestration.message.domain.model

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.protocol.message.OperationMessage

/** A decrypted group plaintext: either a user message or an operation on one. */
sealed interface GroupMessagePlaintext {
    data class Message(
        val text: String,
        val attachments: List<MessagePart>,
        val replyToMessageId: String?
    ) : GroupMessagePlaintext

    data class Operation(
        val message: OperationMessage
    ) : GroupMessagePlaintext
}
