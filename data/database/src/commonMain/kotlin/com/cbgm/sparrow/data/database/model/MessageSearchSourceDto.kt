package com.cbgm.sparrow.data.database.model

data class MessageSearchSourceDto(
    val messageId: String,
    val conversationId: String,
    val conversationType: String,
    val contactId: String?,
    val senderName: String?,
    val conversationTitle: String?,
    val contactName: String?,
    val text: String,
    val createdAtEpochMilliseconds: Long
)
