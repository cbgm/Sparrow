package com.cbgm.sparrow.feature.search.domain.model

data class MessageSearchResult(
    val messageId: String,
    val conversationId: String,
    val conversationType: MessageSearchConversationType,
    val contactId: String?,
    val conversationName: String?,
    val text: String,
    val createdAtEpochMilliseconds: Long,
    val matchType: MessageSearchMatchType,
    val score: Float? = null
)
