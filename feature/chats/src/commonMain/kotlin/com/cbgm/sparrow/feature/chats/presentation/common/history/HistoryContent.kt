package com.cbgm.sparrow.feature.chats.presentation.common.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.rememberDelayedVisibility
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.HistoryEmptyContent
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.HistoryLoadingContent
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.HistorySenderAvatar
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.MessageList
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.rememberDissolvingMessageListState
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.HistoryUiModel
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageContextAnchor
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageHistoryUiState
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageReactionBurst
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

@Composable
internal fun HistoryContent(
    model: HistoryUiModel,
    listState: LazyListState,
    innerPadding: PaddingValues,
    targetMessageId: String?,
    selectedContextMessageId: String?,
    historyState: MessageHistoryUiState,
    onLoadOlderMessages: () -> Unit,
    onMessageHistoryTargetRequested: (String) -> Unit,
    onContextMessageRequested: (MessageContextAnchor) -> Unit,
    onReactionBurstRequested: (MessageReactionBurst) -> Unit,
    onRetryMessage: (String) -> Unit,
    onSafetyWarningClick: (String, String?, MessageSafetyWarningUi) -> Unit,
    onAttachmentClick: (String, String) -> Unit,
    onContactClick: (SharedContact) -> Unit
) {
    val fillModifier = Modifier.fillMaxSize().padding(innerPadding)
    val dissolvingListState =
        rememberDissolvingMessageListState(
            messages = model.messages,
            idOf = { it.id },
            shouldDissolve = { !it.isMine }
        )

    // Do not briefly show a spinner or "no messages" before the first DB emission arrives.
    // Visible messages are always rendered immediately; only placeholders are delayed.
    val showLoading = rememberDelayedVisibility(model.isLoading)
    val showEmpty = rememberDelayedVisibility(
        !model.isLoading && dissolvingListState.messages.isEmpty()
    )

    when {
        model.isLoading -> {
            if (showLoading) HistoryLoadingContent(modifier = fillModifier)
        }

        dissolvingListState.messages.isEmpty() -> {
            if (showEmpty) {
                HistoryEmptyContent(
                    title = model.emptyTitle,
                    description = model.emptyDescription,
                    modifier = fillModifier
                )
            }
        }

        else -> MessageList(
            dissolvingListState = dissolvingListState,
            listState = listState,
            targetMessageId = targetMessageId,
            selectedContextMessageId = selectedContextMessageId,
            onContextMessageRequested = onContextMessageRequested,
            onReactionBurstRequested = onReactionBurstRequested,
            onRetryMessage = onRetryMessage,
            onSafetyWarningClick = onSafetyWarningClick,
            onAttachmentClick = onAttachmentClick,
            onContactClick = onContactClick,
            contentPadding = innerPadding,
            historyState = historyState,
            onLoadOlderMessages = onLoadOlderMessages,
            onMessageHistoryTargetRequested = onMessageHistoryTargetRequested,
            itemLeadingContent =
                if (model.showSenderAvatars) {
                    { message -> HistorySenderAvatar(message) }
                } else {
                    null
                }
        )
    }
}

@Preview
@Composable
private fun HistoryContentPreview() {
    SparrowTheme {
        HistoryContent(
            model = HistoryUiModel(
                messages = emptyList(),
                isLoading = false,
                emptyTitle = "dfsf",
                emptyDescription = "ddfsd  ff sf",
                showSenderAvatars = false
            ),
            listState = rememberLazyListState(),
            innerPadding = PaddingValues(),
            targetMessageId = null,
            selectedContextMessageId = null,
            historyState = MessageHistoryUiState(
                isLoadingOlder = false,
                hasMore = false,
                loadedThroughMessageId = null
            ),
            onLoadOlderMessages = {},
            onMessageHistoryTargetRequested = {},
            onContextMessageRequested = {},
            onReactionBurstRequested = {},
            onRetryMessage = {},
            onSafetyWarningClick = { _, _, _ -> },
            onAttachmentClick = { _, _ -> },
            onContactClick = {}
        )
    }
}
