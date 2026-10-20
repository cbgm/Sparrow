package com.cbgm.sparrow.feature.conversationorchestration.message.domain.model

import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart

/** Validated message content: an optional trimmed text and the non-text parts. */
data class OutgoingMessageContent(
    val text: String,
    val attachments: List<MessagePart>
)
