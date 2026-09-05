package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageHistoryUiState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageReactionUi
import com.cbgm.sparrow.feature.chats.presentation.group.component.MembershipSystemMessage
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

private const val LOAD_MORE_THRESHOLD = 8

@Composable
internal fun MessageList(
    dissolvingListState: DissolvingMessageListState<MessageBubbleUi>,
    listState: LazyListState,
    targetMessageId: String?,
    selectedContextMessageId: String?,
    onContextMessageRequested: (MessageContextAnchor) -> Unit,
    onReactionBurstRequested: (MessageReactionBurst) -> Unit,
    onRetryMessage: (String) -> Unit,
    onSafetyWarningClick: (String, String?, MessageSafetyWarningUi) -> Unit,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String, String) -> Unit,
    onContactClick: (SharedContact) -> Unit,
    contentPadding: PaddingValues,
    historyState: MessageHistoryUiState,
    onLoadOlderMessages: () -> Unit,
    onMessageHistoryTargetRequested: (String) -> Unit,
    itemLeadingContent: (@Composable (MessageBubbleUi) -> Unit)? = null
) {
    val messages = dissolvingListState.messages

    val messageIds = remember(messages) { messages.map(MessageBubbleUi::id) }

    val searchTargetState =
        rememberMessageSearchTargetState(
            targetMessageId = targetMessageId,
            messageIds = messageIds,
            listState = listState
        )
    val replyJumpState =
        rememberMessageJumpState(
            messageIds = messageIds,
            listState = listState,
            onTargetMissing = onMessageHistoryTargetRequested
        )
    AutoScrollToNewestOwnMessage(
        messages = messages,
        searchTargetHandled = searchTargetState.isHandled,
        listState = listState
    )

    HandleMessageHistoryPagination(
        listState = listState,
        historyState = historyState,
        messageCount = messages.size,
        onLoadOlderMessages = onLoadOlderMessages
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        reverseLayout = true,
        contentPadding =
            PaddingValues(
                start = MaterialTheme.spacing.messageList.horizontalPadding,
                top = contentPadding.calculateTopPadding() + MaterialTheme.spacing.small,
                end = MaterialTheme.spacing.messageList.horizontalPadding,
                bottom = contentPadding.calculateBottomPadding() + MaterialTheme.spacing.small
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        items(items = messages, key = MessageBubbleUi::id) { message ->
            MessageDissolve(
                messageId = message.id,
                state = dissolvingListState
            ) {
                val systemType = message.groupExtension?.type
                if (systemType != null && systemType != ChatMessageType.USER) {
                    MembershipSystemMessage(
                        type = systemType,
                        memberName = message.senderName
                    )
                } else {
                    MessageBubble(
                        message = message,
                        onRetryClick = { onRetryMessage(message.id) },
                        onSafetyDetailsClick = { warning ->
                            onSafetyWarningClick(
                                message.id,
                                message.groupExtension?.senderContactId,
                                warning
                            )
                        },
                        onAttachmentVisible = onAttachmentVisible,
                        onAttachmentClick = { attachmentId ->
                            onAttachmentClick(
                                message.id,
                                attachmentId
                            )
                        },
                        onContactClick = onContactClick,
                        onReplyPreviewClick = replyJumpState.jumpTo,
                        onContextMessageRequested = onContextMessageRequested,
                        onReactionsClick = { anchor ->
                            onReactionBurstRequested(
                                MessageReactionBurst(reactions = message.reactions, anchor = anchor)
                            )
                        },
                        isContextSelected = selectedContextMessageId == message.id,
                        isSearchHighlighted =
                            message.id == searchTargetState.highlightedMessageId ||
                                message.id == replyJumpState.highlightedMessageId,
                        leadingContent = {
                            if (!message.isMine) {
                                itemLeadingContent?.invoke(message)
                            }
                        }
                    )
                }
            }
        }
        if (historyState.isLoadingOlder) {
            item(key = "history-loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.micro),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.MessageList.loadingSize),
                        strokeWidth = Dimens.MessageList.loadingStroke
                    )
                }
            }
        }
    }
}

@Composable
private fun HandleMessageHistoryPagination(
    listState: LazyListState,
    historyState: MessageHistoryUiState,
    messageCount: Int,
    onLoadOlderMessages: () -> Unit
) {
    val shouldLoadOlderMessages by remember(
        listState,
        historyState.hasMore,
        historyState.isLoadingOlder,
        messageCount
    ) {
        derivedStateOf {
            if (
                !historyState.hasMore ||
                historyState.isLoadingOlder ||
                messageCount == 0
            ) {
                return@derivedStateOf false
            }

            val lastVisibleIndex =
                listState.layoutInfo.visibleItemsInfo
                    .maxOfOrNull { it.index }
                    ?: return@derivedStateOf false

            lastVisibleIndex >=
                listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(
        shouldLoadOlderMessages,
        historyState.loadedThroughMessageId
    ) {
        if (shouldLoadOlderMessages) {
            onLoadOlderMessages()
        }
    }
}

@Composable
private fun AutoScrollToNewestOwnMessage(
    messages: List<MessageBubbleUi>,
    searchTargetHandled: Boolean,
    listState: LazyListState
) {
    val newestMessage = messages.firstOrNull()

    LaunchedEffect(newestMessage?.id) {
        if (
            searchTargetHandled &&
            newestMessage?.isMine == true
        ) {
            listState.animateScrollToItem(index = 0)
        }
    }
}

@Preview(heightDp = 700)
@Composable
private fun MessageListPreview() {
    SparrowTheme {
        val messages =
            remember {
                listOf(
                    MessageBubbleUi(
                        id = "4",
                        isMine = true,
                        security = MessageSecurity.END_TO_END_ENCRYPTED,
                        contentStatus = MessageContentStatus.READABLE,
                        deliveryStatus = MessageDeliveryStatus.READ,
                        textPart =
                            MessagePartUi.Text(
                                text = "Yes, that sounds good 👍",
                                isContentFailed = false
                            )
                    ),
                    MessageBubbleUi(
                        id = "3",
                        isMine = false,
                        security = MessageSecurity.END_TO_END_ENCRYPTED,
                        contentStatus = MessageContentStatus.READABLE,
                        deliveryStatus = MessageDeliveryStatus.READ,
                        senderName = "Chris",
                        textPart =
                            MessagePartUi.Text(
                                text = "Should we meet around 18:00?",
                                isContentFailed = false
                            )
                    ),
                    MessageBubbleUi(
                        id = "2",
                        isMine = true,
                        security = MessageSecurity.END_TO_END_ENCRYPTED,
                        contentStatus = MessageContentStatus.READABLE,
                        deliveryStatus = MessageDeliveryStatus.DELIVERED,
                        textPart =
                            MessagePartUi.Text(
                                text = "I'm free this evening.",
                                isContentFailed = false
                            )
                    ),
                    MessageBubbleUi(
                        id = "1",
                        isMine = false,
                        security = MessageSecurity.END_TO_END_ENCRYPTED,
                        contentStatus = MessageContentStatus.READABLE,
                        deliveryStatus = MessageDeliveryStatus.READ,
                        senderName = "Chris",
                        reactions =
                            listOf(
                                MessageReactionUi(
                                    emoji = "❤️",
                                    count = 2,
                                    reactedByMe = false
                                )
                            ),
                        textPart =
                            MessagePartUi.Text(
                                text = "Hey! How are you?",
                                isContentFailed = false
                            )
                    )
                )
            }

        val dissolvingListState =
            rememberDissolvingMessageListState(
                messages = messages,
                idOf = MessageBubbleUi::id,
                shouldDissolve = { false }
            )

        MessageList(
            dissolvingListState = dissolvingListState,
            listState = rememberLazyListState(),
            targetMessageId = null,
            selectedContextMessageId = null,
            onContextMessageRequested = {},
            onReactionBurstRequested = {},
            onRetryMessage = {},
            onSafetyWarningClick = { _, _, _ -> },
            onAttachmentVisible = {},
            onAttachmentClick = { _, _ -> },
            onContactClick = {},
            contentPadding = PaddingValues(),
            historyState = MessageHistoryUiState(
                isLoadingOlder = true,
                hasMore = true,
                loadedThroughMessageId = "4"
            ),
            onLoadOlderMessages = {},
            onMessageHistoryTargetRequested = {}
        )
    }
}
