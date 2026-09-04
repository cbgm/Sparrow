package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.component.SparrowAvatar
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.group.component.MembershipSystemMessage
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

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
    contentPadding: PaddingValues
) {
    val messages = dissolvingListState.messages
    val searchTargetState =
        rememberMessageSearchTargetState(
            targetMessageId = targetMessageId,
            messageIds = messages.map(MessageBubbleUi::id),
            listState = listState
        )
    val replyJumpState =
        rememberMessageJumpState(
            messageIds = messages.map(MessageBubbleUi::id),
            listState = listState
        )
    val newestMessage = messages.firstOrNull()
    LaunchedEffect(newestMessage?.id) {
        if (searchTargetState.isHandled && newestMessage?.isMine == true) {
            listState.animateScrollToItem(index = 0)
        }
    }

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
                            if (message.groupExtension != null) {
                                BubbleLeadingContent(message)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BubbleLeadingContent(message: MessageBubbleUi) {
    if (!message.isMine) {
        SparrowAvatar(
            name = message.senderName.orEmpty(),
            pictureBytes = message.groupExtension?.senderProfilePictureBytes,
            size = Dimens.GroupScreen.typingAvatarSize
        )

        Spacer(
            modifier = Modifier.width(
                MaterialTheme.spacing.groupScreen.senderGap
            )
        )
    }
}
