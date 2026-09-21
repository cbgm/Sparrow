package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cbgm.sparrow.core.ui.component.FeedbackOverlay
import com.cbgm.sparrow.core.ui.component.FeedbackOverlayData
import com.cbgm.sparrow.core.ui.component.PatternBackground
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowOverlay
import com.cbgm.sparrow.core.ui.component.SparrowOverlayHost
import com.cbgm.sparrow.core.ui.device.clipboard.rememberClipboardWriter
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.presentation.component.MessageAttachmentViewer
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.presentation.common.composer.ComposerContent
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.IndicatorUiState
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.MessageComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.common.header.HeaderContent
import com.cbgm.sparrow.feature.chats.presentation.common.header.component.GroupPinnedMessageBar
import com.cbgm.sparrow.feature.chats.presentation.common.header.component.GroupPinnedMessageContent
import com.cbgm.sparrow.feature.chats.presentation.common.header.component.StatusHint
import com.cbgm.sparrow.feature.chats.presentation.common.header.component.subtitle
import com.cbgm.sparrow.feature.chats.presentation.common.header.mapper.toHeaderUiModel
import com.cbgm.sparrow.feature.chats.presentation.common.history.HistoryContent
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.AddSharedContactDialog
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.HistorySenderAvatar
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.MessageBubble
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.MessageContextHost
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.MessageReactionBurstOverlay
import com.cbgm.sparrow.feature.chats.presentation.common.history.mapper.toHistoryUiModel
import com.cbgm.sparrow.feature.chats.presentation.common.history.mapper.toMessageAttachmentsUi
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageContextAnchor
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageContextUiState
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageHistoryUiState
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageReactionBurst
import com.cbgm.sparrow.feature.chats.presentation.component.mapper.toSharedContact
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiEvent
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMembershipUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.findMessage
import com.cbgm.sparrow.feature.contacts.presentation.overview.ContactAttachmentSelectionRoute
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.common_copied
import com.cbgm.sparrow.resources.feature_chats_no_messages_yet
import org.jetbrains.compose.resources.stringResource

/**
 * A message + the attachment within it that is currently open in the full-screen viewer.
 * Replaces the previous pair of nullable (messageId, attachmentId) states.
 */
private data class AttachmentSelection(
    val message: MessageBubbleUi,
    val attachmentId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupConversationScreen(
    uiState: GroupConversationUiState,
    composerState: MessageComposerUiState,
    contextState: MessageContextUiState<MessageBubbleUi>,
    indicatorState: IndicatorUiState,
    membershipState: GroupMembershipUiState,
    historyState: MessageHistoryUiState,
    onUiEvent: (GroupConversationUiEvent) -> Unit,
    onForwardMessageRequested: (String) -> Unit,
    modifier: Modifier = Modifier,
    targetMessageId: String? = null
) {
    var attachmentSelection by remember {
        mutableStateOf<AttachmentSelection?>(null)
    }
    var showContactSelection by rememberSaveable { mutableStateOf(false) }
    var showPinnedMessage by rememberSaveable { mutableStateOf(false) }
    var pendingSharedContact by remember { mutableStateOf<SharedContact?>(null) }
    var messageContextAnchor by remember { mutableStateOf<MessageContextAnchor?>(null) }
    var reactionBurst by remember { mutableStateOf<MessageReactionBurst?>(null) }
    var feedbackOverlay by remember { mutableStateOf<FeedbackOverlayData?>(null) }

    LaunchedEffect(uiState.pinnedMessage) {
        if (uiState.pinnedMessage == null) showPinnedMessage = false
    }

    val contextMessage = contextState.message
    val clipboardWriter = rememberClipboardWriter()
    val copiedText = stringResource(Res.string.common_copied)
    val contextMenuColor =
        if (contextMessage?.isMine == true) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }

    val activeContextAnchor =
        messageContextAnchor?.takeIf { anchor -> anchor.messageId == contextMessage?.id }

    Box(modifier = modifier.fillMaxSize()) {
        MessageContextHost(
            anchor = activeContextAnchor,
            menuColor = contextMenuColor,
            onDismiss = {
                messageContextAnchor = null
                onUiEvent(GroupConversationUiEvent.MessageContextDismissed)
            },
            onReplyClick = {
                contextMessage?.id?.let { onUiEvent(GroupConversationUiEvent.ReplyToMessage(it)) }
            },
            onForwardClick = { contextMessage?.id?.let(onForwardMessageRequested) },
            showPin =
                uiState.isLocalAdmin &&
                    contextMessage?.groupExtension?.type == ChatMessageType.USER,
            isPinned = contextMessage?.id == uiState.pinnedMessage?.id,
            onPinClick = {
                val messageId = contextMessage?.id ?: return@MessageContextHost
                val event =
                    if (messageId == uiState.pinnedMessage?.id) {
                        GroupConversationUiEvent.UnpinMessage
                    } else {
                        GroupConversationUiEvent.PinMessage(messageId)
                    }
                onUiEvent(event)
            },
            onReactionClick = { emoji ->
                contextMessage?.id?.let {
                    onUiEvent(GroupConversationUiEvent.MessageReactionSelected(it, emoji))
                }
            },
            showEdit = contextState.canEdit,
            onEditClick = {
                contextMessage?.id?.let { onUiEvent(GroupConversationUiEvent.EditMessage(it)) }
            },
            onCopyClick = {
                contextMessage?.textPart?.text?.takeIf(String::isNotBlank)
                    ?.let(clipboardWriter::copyText)
                activeContextAnchor?.let { anchor ->
                    feedbackOverlay =
                        FeedbackOverlayData(
                            anchor = anchor.overlayAnchor,
                            text = copiedText,
                            color = contextMenuColor
                        )
                }
            },
            onDeleteClick = {
                contextMessage?.id?.let { onUiEvent(GroupConversationUiEvent.DeleteMessage(it)) }
            },
            modifier = Modifier.fillMaxSize(),
            preview = {
                contextMessage?.let { message ->
                    MessageBubble(
                        message = message,
                        onRetryClick = {},
                        onSafetyDetailsClick = {},
                        onAttachmentClick = {},
                        onContactClick = {},
                        onReplyPreviewClick = {},
                        onContextMessageRequested = {},
                        onReactionsClick = {},
                        isContextSelected = false,
                        isSearchHighlighted = false,
                        showMetadata = false,
                        contextMenuEnabled = false,
                        leadingContent = senderAvatarOrNull(message)
                    )
                }
            }
        ) {
            SparrowLazyScaffold(
                modifier = Modifier.fillMaxSize(),
                barColor = MaterialTheme.colorScheme.background,
                background = {
                    PatternBackground(
                        modifier = Modifier.fillMaxSize(),
                        backgroundColor = MaterialTheme.colorScheme.background,
                        alpha = Alpha.PatternBackground.conversation
                    )
                },
                topBar = { containerColor ->
                    Column {
                        HeaderContent(
                            model = uiState.toHeaderUiModel(subtitle(uiState, membershipState)),
                            containerColor = containerColor,
                            onBackClick = { onUiEvent(GroupConversationUiEvent.BackClicked) },
                            onHeaderClick = { onUiEvent(GroupConversationUiEvent.HeaderClicked) }
                        ) {
                            StatusHint(uiState = uiState, membershipState = membershipState)
                        }
                        uiState.pinnedMessage?.let { pinnedMessage ->
                            GroupPinnedMessageBar(
                                message = pinnedMessage,
                                pinnedAtEpochMilliseconds = uiState.pinnedAtEpochMilliseconds,
                                canUnpin = uiState.isLocalAdmin,
                                onClick = { showPinnedMessage = true },
                                onUnpinClick = { onUiEvent(GroupConversationUiEvent.UnpinMessage) }
                            )
                        }
                    }
                },
                bottomBar = { containerColor ->
                    BottomBar(
                        composerState = composerState,
                        indicatorState = indicatorState,
                        containerColor = containerColor,
                        onUiEvent = onUiEvent,
                        onContactAttachmentClick = { showContactSelection = true }
                    )
                }
            ) { innerPadding, listState ->
                HistoryContent(
                    model = uiState.toHistoryUiModel(
                        emptyDescription = stringResource(Res.string.feature_chats_no_messages_yet)
                    ),
                    listState = listState,
                    innerPadding = innerPadding,
                    targetMessageId = targetMessageId,
                    selectedContextMessageId = messageContextAnchor?.messageId,
                    historyState = historyState,
                    onLoadOlderMessages = { onUiEvent(GroupConversationUiEvent.LoadOlderMessages) },
                    onMessageHistoryTargetRequested = {
                        onUiEvent(GroupConversationUiEvent.MessageHistoryTargetRequested(it))
                    },
                    onContextMessageRequested = { anchor ->
                        messageContextAnchor = anchor
                        onUiEvent(GroupConversationUiEvent.MessageContextRequested(anchor.messageId))
                    },
                    onReactionBurstRequested = { reactionBurst = it },
                    onRetryMessage = { onUiEvent(GroupConversationUiEvent.RetryMessage(it)) },
                    onSafetyWarningClick = { messageId, contactId, warning ->
                        onUiEvent(
                            GroupConversationUiEvent.SafetyWarningClicked(
                                messageId = messageId,
                                contactId = contactId,
                                warning = warning
                            )
                        )
                    },
                    onAttachmentClick = { messageId, attachmentId ->
                        uiState.findMessage(messageId)?.let { message ->
                            attachmentSelection =
                                AttachmentSelection(
                                    message = message,
                                    attachmentId = attachmentId
                                )
                        }
                    },
                    onContactClick = { contact -> pendingSharedContact = contact }
                )
            }
        }

        feedbackOverlay?.let { feedback ->
            SparrowOverlay(anchor = feedback.anchor) {
                FeedbackOverlay(
                    text = feedback.text,
                    color = feedback.color,
                    onDismiss = { feedbackOverlay = null }
                )
            }
        }

        reactionBurst?.let { burst ->
            SparrowOverlay(anchor = burst.anchor) {
                MessageReactionBurstOverlay(burst = burst, onDismiss = { reactionBurst = null })
            }
        }
    }

    SparrowOverlayHost(
        visible = showContactSelection,
        onDismissRequest = { showContactSelection = false },
        horizontalPadding = MaterialTheme.spacing.zero,
        topPadding = MaterialTheme.spacing.times(6)
    ) { dismissOverlay ->
        ContactAttachmentSelectionRoute(
            onContactSelected = { contact ->
                contact.toSharedContact()?.let { sharedContact ->
                    dismissOverlay()
                    onUiEvent(GroupConversationUiEvent.ShareContact(sharedContact))
                }
            },
            onBack = dismissOverlay,
            modifier = Modifier.fillMaxSize()
        )
    }

    GroupPinnedMessageOverlay(
        visible = showPinnedMessage,
        message = uiState.pinnedMessage,
        onDismissRequest = { showPinnedMessage = false },
        onAttachmentClick = { _, attachmentId ->
            uiState.pinnedMessage?.let { message ->
                attachmentSelection =
                    AttachmentSelection(
                        message = message,
                        attachmentId = attachmentId
                    )
            }
        },
        onContactClick = { contact -> pendingSharedContact = contact }
    )

    pendingSharedContact?.let { contact ->
        AddSharedContactDialog(
            contact = contact,
            onConfirm = {
                pendingSharedContact = null
                onUiEvent(GroupConversationUiEvent.AddSharedContact(contact))
            },
            onDismiss = { pendingSharedContact = null }
        )
    }

    attachmentSelection?.let { selection ->
        AttachmentViewerOverlay(
            message = selection.message,
            attachmentId = selection.attachmentId,
            onDismiss = { attachmentSelection = null },
            onUiEvent = onUiEvent
        )
    }
}

@Composable
private fun AttachmentViewerOverlay(
    message: MessageBubbleUi,
    attachmentId: String,
    onDismiss: () -> Unit,
    onUiEvent: (GroupConversationUiEvent) -> Unit
) {
    MessageAttachmentViewer(
        attachments = message.toMessageAttachmentsUi(),
        selectedAttachmentId = attachmentId,
        canSaveToCameraRoll = !message.isMine,
        onDismiss = onDismiss,
        onError = { onUiEvent(GroupConversationUiEvent.AttachmentError(it)) }
    )
}

/** Avatar + spacer leading content for a non-own message bubble, or null for own messages. */
@Composable
private fun senderAvatarOrNull(message: MessageBubbleUi): (@Composable () -> Unit)? =
    if (message.isMine) {
        null
    } else {
        { HistorySenderAvatar(message) }
    }

@Composable
private fun GroupPinnedMessageOverlay(
    visible: Boolean,
    message: MessageBubbleUi?,
    onDismissRequest: () -> Unit,
    onAttachmentClick: (String, String) -> Unit,
    onContactClick: (SharedContact) -> Unit
) {
    SparrowOverlayHost(
        visible = visible && message != null,
        onDismissRequest = onDismissRequest,
        horizontalPadding = MaterialTheme.spacing.zero,
        topPadding = MaterialTheme.spacing.times(6)
    ) { dismissOverlay ->
        message?.let { pinnedMessage ->
            GroupPinnedMessageContent(
                message = pinnedMessage,
                onBack = dismissOverlay,
                onAttachmentClick = { attachmentId ->
                    onAttachmentClick(pinnedMessage.id, attachmentId)
                },
                onContactClick = onContactClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun BottomBar(
    composerState: MessageComposerUiState,
    indicatorState: IndicatorUiState,
    containerColor: Color,
    onUiEvent: (GroupConversationUiEvent) -> Unit,
    onContactAttachmentClick: () -> Unit
) {
    ComposerContent(
        composerState = composerState,
        indicatorState = indicatorState,
        containerColor = containerColor,
        onMessageTextChanged = { onUiEvent(GroupConversationUiEvent.MessageTextChanged(it)) },
        onSendClick = { onUiEvent(GroupConversationUiEvent.SendClicked) },
        onCancelReply = { onUiEvent(GroupConversationUiEvent.CancelReply) },
        onCancelEdit = { onUiEvent(GroupConversationUiEvent.CancelEdit) },
        onMediaSelected = { onUiEvent(GroupConversationUiEvent.MediaSelected(it)) },
        onOpenFilePicker = { onUiEvent(GroupConversationUiEvent.OpenFilePicker(it)) },
        onContactAttachmentClick = onContactAttachmentClick,
        onLocationCaptureStarted = { onUiEvent(GroupConversationUiEvent.LocationCaptureStarted) },
        onLocationCaptured = { onUiEvent(GroupConversationUiEvent.ShareCurrentLocation(it)) },
        onLocationCaptureFailed = { onUiEvent(GroupConversationUiEvent.LocationCaptureFailed(it)) },
        onAttachmentError = { onUiEvent(GroupConversationUiEvent.AttachmentError(it)) },
        onVoiceSendClick = { onUiEvent(GroupConversationUiEvent.VoiceSendClicked) }
    )
}
