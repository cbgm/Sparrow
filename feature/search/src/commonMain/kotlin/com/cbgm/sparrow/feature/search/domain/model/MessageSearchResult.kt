package com.cbgm.sparrow.feature.search.domain.model

data class MessageSearchResult(
    val messageId: String,
    val conversationId: String,
    val text: String,
    val createdAtEpochMilliseconds: Long,
    val score: Float
)
