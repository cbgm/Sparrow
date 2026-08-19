package com.cbgm.sparrow.data.database.model

data class MessageSearchSource(
    val messageId: String,
    val conversationId: String,
    val text: String,
    val createdAtEpochMilliseconds: Long
)
