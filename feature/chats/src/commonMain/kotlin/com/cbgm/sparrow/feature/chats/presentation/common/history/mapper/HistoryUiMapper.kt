package com.cbgm.sparrow.feature.chats.presentation.common.history.mapper

import com.cbgm.sparrow.feature.chats.presentation.common.history.model.HistoryUiModel
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState

internal fun DirectConversationUiState.toHistoryUiModel(
    emptyTitle: String,
    emptyDescription: String
): HistoryUiModel =
    HistoryUiModel(
        messages = messages,
        isLoading = isLoading,
        emptyTitle = emptyTitle,
        emptyDescription = emptyDescription
    )

internal fun GroupConversationUiState.toHistoryUiModel(
    emptyDescription: String
): HistoryUiModel =
    HistoryUiModel(
        messages = messages,
        isLoading = isLoading,
        emptyTitle = title,
        emptyDescription = emptyDescription,
        showSenderAvatars = true
    )
