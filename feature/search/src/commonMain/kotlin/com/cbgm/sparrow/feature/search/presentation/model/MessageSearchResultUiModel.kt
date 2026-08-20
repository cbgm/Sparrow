package com.cbgm.sparrow.feature.search.presentation.model

data class MessageSearchResultUiModel(
    val messageId: String,
    val conversationId: String,
    val conversationName: String?,
    val text: String,
    val timestamp: String
)
