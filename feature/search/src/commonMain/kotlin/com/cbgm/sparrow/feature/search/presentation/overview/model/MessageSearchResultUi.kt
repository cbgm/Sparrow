package com.cbgm.sparrow.feature.search.presentation.overview.model

import com.cbgm.sparrow.feature.search.domain.model.MessageSearchConversationType

data class MessageSearchResultUi(
    val messageId: String,
    val conversationId: String,
    val conversationType: MessageSearchConversationType,
    val contactId: String?,
    val conversationName: String?,
    val text: String,
    val timestamp: String
)
