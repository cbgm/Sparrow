package com.cbgm.sparrow.data.database.model

data class StoredMessageSearchMatchDto(
    val messageId: String,
    val conversationId: String,
    val conversationType: String,
    val contactId: String?,
    val conversationTitle: String?,
    val contactName: String?,
    val text: String,
    val createdAtEpochMilliseconds: Long
)
