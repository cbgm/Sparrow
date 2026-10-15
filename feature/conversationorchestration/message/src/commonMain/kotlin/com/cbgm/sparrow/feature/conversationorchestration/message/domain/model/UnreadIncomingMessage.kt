package com.cbgm.sparrow.feature.conversationorchestration.message.domain.model

data class UnreadIncomingMessage(
    val messageId: String,
    val conversationId: String,
    val contactId: String
)
