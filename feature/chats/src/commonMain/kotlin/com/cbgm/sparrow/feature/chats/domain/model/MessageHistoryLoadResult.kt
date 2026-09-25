package com.cbgm.sparrow.feature.chats.domain.model

data class MessageHistoryLoadResult(
    val oldestCursor: MessageHistoryCursor?,
    val hasMore: Boolean
)
