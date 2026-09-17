package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cbgm.sparrow.core.ui.component.FeedbackOverlay
import com.cbgm.sparrow.core.ui.component.FeedbackOverlayData
import com.cbgm.sparrow.core.ui.component.PatternBackground
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowOverlay
import com.cbgm.sparrow.core.ui.component.SparrowOverlayHost
import com.cbgm.sparrow.core.ui.device.clipboard.rememberClipboardWriter
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.presentation.component.MessageAttachmentViewer
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.presentation.component.AddSharedContactDialog
import com.cbgm.sparrow.feature.chats.presentation.component.ChatComposerBar
import com.cbgm.sparrow.feature.chats.presentation.component.MessageBubble
import com.cbgm.sparrow.feature.chats.presentation.component.MessageContextAnchor
import com.cbgm.sparrow.feature.chats.presentation.component.MessageContextHost
import com.cbgm.sparrow.feature.chats.presentation.component.MessageList
import com.cbgm.sparrow.feature.chats.presentation.component.MessageReactionBurst
import com.cbgm.sparrow.feature.chats.presentation.component.MessageReactionBurstOverlay
import com.cbgm.sparrow.feature.chats.presentation.component.mapper.toMessageAttachmentsUi
import com.cbgm.sparrow.feature.chats.presentation.component.mapper.toSharedContact
import com.cbgm.sparrow.feature.chats.presentation.component.model.IndicatorUiState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageContextUiState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageHistoryUiState
import com.cbgm.sparrow.feature.chats.presentation.component.rememberDissolvingMessageListState
import com.cbgm.sparrow.feature.chats.presentation.group.component.GroupPinnedMessageBar
import com.cbgm.sparrow.feature.chats.presentation.group.component.GroupPinnedMessageContent
import com.cbgm.sparrow.feature.chats.presentation.group.component.StatusHint
import com.cbgm.sparrow.feature.chats.presentation.group.component.subtitle
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiEvent
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMembershipUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.findMessage
import com.cbgm.sparrow.feature.contacts.presentation.overview.ContactAttachmentSelectionRoute
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.common_copied
import com.cbgm.sparrow.resources.feature_chats_loading_chat
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
    errorMessage: String?,
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message -> snackbarHostState.showSnackbar(message) }
    }

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
                snackbarHostState = snackbarHostState,
                background = {
                    PatternBackground(
                        modifier = Modifier.fillMaxSize(),
                        backgroundColor = MaterialTheme.colorScheme.background,
                        alpha = Alpha.PatternBackground.conversation
                    )
                },
                topBar = { containerColor ->
                    Column {
                        TopBar(
                            uiState = uiState,
                            membershipState = membershipState,
                            containerColor = containerColor,
                            onUiEvent = onUiEvent
                        )
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
                Content(
                    uiState = uiState,
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
private fun senderAvatarOrNull(message: MessageBubbleUi): (@Composable () -> Unit)? {
    if (message.isMine) return null
    return {
        SparrowAvatar(
            name = message.senderName.orEmpty(),
            target = message.groupExtension?.senderContactId
                ?.takeIf(String::isNotBlank)
                ?.let { AvatarTarget.User(it) },
            size = Dimens.GroupConversationScreen.avatarSize
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.groupConversationScreen.senderGap))
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    uiState: GroupConversationUiState,
    membershipState: GroupMembershipUiState,
    containerColor: Color,
    onUiEvent: (GroupConversationUiEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
            title = {
                Row(
                    modifier = Modifier.clickable { onUiEvent(GroupConversationUiEvent.HeaderClicked) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SparrowAvatar(
                        name = uiState.title,
                        target = uiState.groupId
                            .takeIf(String::isNotBlank)
                            ?.let { AvatarTarget.Group(it) },
                        size = Dimens.GroupConversationScreen.topBarAvatarSize
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Column {
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle(uiState, membershipState),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { onUiEvent(GroupConversationUiEvent.BackClicked) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        )

        StatusHint(
            uiState = uiState,
            membershipState = membershipState
        )
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
    ChatComposerBar(
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

@Composable
private fun Content(
    uiState: GroupConversationUiState,
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
    val dissolvingMessageState =
        rememberDissolvingMessageListState(
            messages = uiState.messages,
            idOf = { message -> message.id },
            shouldDissolve = { message -> !message.isMine }
        )

    when {
        uiState.isLoading -> LoadingContent(modifier = fillModifier)

        dissolvingMessageState.messages.isEmpty() -> EmptyContent(
            title = uiState.title,
            modifier = fillModifier
        )

        else -> MessageList(
            dissolvingListState = dissolvingMessageState,
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
            itemLeadingContent = { message ->
                SparrowAvatar(
                    name = message.senderName.orEmpty(),
                    target = message.groupExtension?.senderContactId
                        ?.takeIf(String::isNotBlank)
                        ?.let { AvatarTarget.User(it) },
                    size = Dimens.GroupConversationScreen.avatarSize,
                    modifier = Modifier.padding(end = MaterialTheme.spacing.groupConversationScreen.senderGap)
                )
            }
        )
    }
}

@Composable
private fun EmptyContent(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(Res.string.feature_chats_no_messages_yet),
                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Text(
            text = stringResource(Res.string.feature_chats_loading_chat),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
