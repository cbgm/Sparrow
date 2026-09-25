package com.cbgm.sparrow.feature.chats.presentation.common.history.model

/** Common history data. Conversation-specific state is mapped before entering a composable. */
data class HistoryUiModel(
    val messages: List<MessageBubbleUi>,
    val isLoading: Boolean,
    val emptyTitle: String,
    val emptyDescription: String,
    val showSenderAvatars: Boolean = false
)
