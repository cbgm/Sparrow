package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart

/** The readable content of an incoming chat message. */
data class IncomingMessageContent(
    val text: String,
    val attachments: List<MessagePart>,
    val replyToMessageId: String?
)
