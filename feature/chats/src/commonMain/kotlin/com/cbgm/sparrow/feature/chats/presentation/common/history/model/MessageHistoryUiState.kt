package com.cbgm.sparrow.feature.chats.presentation.common.history.model

data class MessageHistoryUiState(
    val isLoadingOlder: Boolean = false,
    val hasMore: Boolean = true,
    val loadedThroughMessageId: String? = null,
    /** Every day of the conversation that has messages, newest first. */
    val days: List<MessageDayUi> = emptyList()
)

data class MessageDayUi(
    val epochDay: Long,
    val firstMessageId: String,
    /** When the first message of the day was written, in epoch milliseconds. */
    val firstMessageAt: Long,
    val messageCount: Int
)
