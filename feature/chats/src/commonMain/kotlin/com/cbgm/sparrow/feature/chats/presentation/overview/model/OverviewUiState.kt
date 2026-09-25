package com.cbgm.sparrow.feature.chats.presentation.overview.model

data class OverviewUiState(
    val conversations: List<ConversationListItem> = emptyList(),
    val activeAutoReplyName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = ""
)
