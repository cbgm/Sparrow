package com.cbgm.sparrow.feature.chats.presentation.common.history.mapper

import com.cbgm.sparrow.feature.chats.presentation.common.history.model.HistoryUi
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState

internal fun DirectConversationUiState.toHistoryUi(
    emptyTitle: String,
    emptyDescription: String
): HistoryUi =
    HistoryUi(
        messages = messages,
        isLoading = isLoading,
        emptyTitle = emptyTitle,
        emptyDescription = emptyDescription
    )

internal fun GroupConversationUiState.toHistoryUi(
    emptyDescription: String
): HistoryUi =
    HistoryUi(
        messages = messages,
        isLoading = isLoading,
        emptyTitle = title,
        emptyDescription = emptyDescription,
        showSenderAvatars = true
    )
