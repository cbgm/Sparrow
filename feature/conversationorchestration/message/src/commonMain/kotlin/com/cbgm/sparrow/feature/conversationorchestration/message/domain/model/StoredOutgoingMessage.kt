package com.cbgm.sparrow.feature.conversationorchestration.message.domain.model

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart

/** A persisted outgoing message with its persisted (attachment-backed) parts. */
data class StoredOutgoingMessage(
    val message: StoredMessage,
    val text: String,
    val parts: List<MessagePart>
)
