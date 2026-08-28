package com.cbgm.sparrow.data.database.model

data class UnreadIncomingMessageDto(
    val messageId: String,
    val conversationId: String,
    val contactId: String
)
