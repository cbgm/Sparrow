package com.cbgm.sparrow.data.database.model

data class StoredMessageSearchMatch(
    val messageId: String,
    val conversationId: String,
    val conversationTitle: String?,
    val contactName: String?,
    val text: String,
    val createdAtEpochMilliseconds: Long
)
