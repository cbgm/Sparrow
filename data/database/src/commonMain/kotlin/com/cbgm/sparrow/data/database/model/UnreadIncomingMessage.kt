package com.cbgm.sparrow.data.database.model

data class UnreadIncomingMessage(
    val messageId: String,
    val conversationId: String,
    val contactId: String
)
