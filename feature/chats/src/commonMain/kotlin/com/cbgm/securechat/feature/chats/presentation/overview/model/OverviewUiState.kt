package com.cbgm.securechat.feature.chats.presentation.overview.model

sealed interface OverviewUiState {
    data object Loading : OverviewUiState

    data object Empty : OverviewUiState

    data class Content(
        val conversations: List<ConversationListItem>
    ) : OverviewUiState

    data class Error(
        val message: String
    ) : OverviewUiState
}
