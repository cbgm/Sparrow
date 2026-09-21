package com.cbgm.sparrow.feature.chats.presentation.common.history.model

data class MessageHistoryUiState(
    val isLoadingOlder: Boolean = false,
    val hasMore: Boolean = true,
    val loadedThroughMessageId: String? = null
)
