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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.PatternBackground
import com.cbgm.sparrow.core.ui.component.SparrowAvatar
import com.cbgm.sparrow.core.ui.component.SparrowBannerButton
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowOverlayHost
import com.cbgm.sparrow.core.ui.device.clipboard.rememberClipboardWriter
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.device.rememberCurrentLocationLauncher
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.presentation.component.MessageAttachmentViewer
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMemberInvitationStatus
import com.cbgm.sparrow.feature.chats.presentation.component.AddSharedContactDialog
import com.cbgm.sparrow.feature.chats.presentation.component.DissolvingMessageListState
import com.cbgm.sparrow.feature.chats.presentation.component.MessageBubble
import com.cbgm.sparrow.feature.chats.presentation.component.MessageContextAnchor
import com.cbgm.sparrow.feature.chats.presentation.component.MessageContextHost
import com.cbgm.sparrow.feature.chats.presentation.component.MessageControl
import com.cbgm.sparrow.feature.chats.presentation.component.MessageDissolve
import com.cbgm.sparrow.feature.chats.presentation.component.MessageInputActions
import com.cbgm.sparrow.feature.chats.presentation.component.MessageInputState
import com.cbgm.sparrow.feature.chats.presentation.component.MessageReactionBurst
import com.cbgm.sparrow.feature.chats.presentation.component.captureMessageContextAnchor
import com.cbgm.sparrow.feature.chats.presentation.component.mapper.toMessageAttachmentsUi
import com.cbgm.sparrow.feature.chats.presentation.component.mapper.toSharedContact
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.feature.chats.presentation.component.rememberDissolvingMessageListState
import com.cbgm.sparrow.feature.chats.presentation.component.rememberMessageJumpState
import com.cbgm.sparrow.feature.chats.presentation.component.rememberMessageSearchTargetState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMessageUi
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupUiEvent
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupUiState
import com.cbgm.sparrow.feature.contacts.presentation.overview.ContactAttachmentSelectionRoute
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResult
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.feature.media.presentation.selection.rememberMediaSelectionLauncher
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_group_accept
import com.cbgm.sparrow.resources.feature_chats_group_decline
import com.cbgm.sparrow.resources.feature_chats_group_deleted_description
import com.cbgm.sparrow.resources.feature_chats_group_deleted_status
import com.cbgm.sparrow.resources.feature_chats_group_deleted_title
import com.cbgm.sparrow.resources.feature_chats_group_invitation_description
import com.cbgm.sparrow.resources.feature_chats_group_invitation_title
import com.cbgm.sparrow.resources.feature_chats_group_leaving_hint_description
import com.cbgm.sparrow.resources.feature_chats_group_leaving_hint_title
import com.cbgm.sparrow.resources.feature_chats_group_member_accepted
import com.cbgm.sparrow.resources.feature_chats_group_member_active
import com.cbgm.sparrow.resources.feature_chats_group_member_added_message
import com.cbgm.sparrow.resources.feature_chats_group_member_count
import com.cbgm.sparrow.resources.feature_chats_group_member_declined
import com.cbgm.sparrow.resources.feature_chats_group_member_expired
import com.cbgm.sparrow.resources.feature_chats_group_member_failed
import com.cbgm.sparrow.resources.feature_chats_group_member_invited
import com.cbgm.sparrow.resources.feature_chats_group_member_key_sent
import com.cbgm.sparrow.resources.feature_chats_group_member_left_message
import com.cbgm.sparrow.resources.feature_chats_group_member_removed_message
import com.cbgm.sparrow.resources.feature_chats_group_message_queued
import com.cbgm.sparrow.resources.feature_chats_group_removed_hint_description
import com.cbgm.sparrow.resources.feature_chats_group_removed_hint_title
import com.cbgm.sparrow.resources.feature_chats_group_status_declined
import com.cbgm.sparrow.resources.feature_chats_group_status_distributing
import com.cbgm.sparrow.resources.feature_chats_group_status_expired
import com.cbgm.sparrow.resources.feature_chats_group_status_failed
import com.cbgm.sparrow.resources.feature_chats_group_status_invited
import com.cbgm.sparrow.resources.feature_chats_group_status_joining
import com.cbgm.sparrow.resources.feature_chats_group_status_leaving
import com.cbgm.sparrow.resources.feature_chats_group_status_partial
import com.cbgm.sparrow.resources.feature_chats_group_status_removed
import com.cbgm.sparrow.resources.feature_chats_group_status_waiting
import com.cbgm.sparrow.resources.feature_chats_group_unknown_member
import com.cbgm.sparrow.resources.feature_chats_group_you_left_message
import com.cbgm.sparrow.resources.feature_chats_group_you_were_removed_message
import com.cbgm.sparrow.resources.feature_chats_loading_chat
import com.cbgm.sparrow.resources.feature_chats_no_messages_yet
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    uiState: GroupUiState,
    onUiEvent: (GroupUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    targetMessageId: String? = null
) {
    var viewerMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    var viewerAttachmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var showContactSelection by rememberSaveable { mutableStateOf(false) }
    var pendingSharedContact by remember { mutableStateOf<SharedContact?>(null) }
    var messageContextAnchor by remember { mutableStateOf<MessageContextAnchor?>(null) }
    var reactionBurst by remember { mutableStateOf<MessageReactionBurst?>(null) }

    val contextMessage =
        messageContextAnchor
            ?.messageId
            ?.let { messageId -> uiState.messages.firstOrNull { it.id == messageId } }
    val contextMessageText = contextMessage?.bubble?.textPart?.text?.takeIf { it.isNotBlank() }
    val clipboardWriter = rememberClipboardWriter()

    val activeContextAnchor =
        messageContextAnchor?.takeIf {
            contextMessage != null
        }

    MessageContextHost(
        anchor = activeContextAnchor,
        menuColor =
            if (contextMessage?.bubble?.isMine == true) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        onDismiss = {
            messageContextAnchor = null
        },
        onReplyClick = {
            contextMessage?.let { message ->
                onUiEvent(
                    GroupUiEvent.ReplyToMessage(
                        message.id
                    )
                )
            }
        },
        onReactionClick = { emoji ->
            contextMessage?.let { message ->
                onUiEvent(GroupUiEvent.MessageReactionSelected(message.id, emoji))
            }
        },
        onCopyClick = {
            contextMessageText?.let(clipboardWriter::copyText)
        },
        onDeleteClick = {
            contextMessage?.let { message ->
                onUiEvent(GroupUiEvent.DeleteMessage(message.id))
            }
        },
        reactionBurst = reactionBurst,
        onReactionBurstDismiss = { reactionBurst = null },
        modifier = modifier,
        preview = {
            contextMessage?.let { message ->
                GroupMessageBubble(
                    message = message,
                    onRetryMessage = {},
                    onSafetyWarningClick = { _, _, _ -> },
                    onAttachmentVisible = {},
                    onAttachmentClick = { _, _ -> },
                    onContactClick = {},
                    onReplyPreviewClick = {},
                    onContextMessageRequested = {},
                    onReactionBurstRequested = {},
                    isContextSelected = false,
                    isSearchHighlighted = false,
                    showMetadata = false,
                    contextMenuEnabled = false
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
                TopBar(
                    uiState = uiState,
                    containerColor = containerColor,
                    onUiEvent = onUiEvent
                )
            },
            bottomBar = { containerColor ->
                BottomBar(
                    uiState = uiState,
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
                onContextMessageRequested = { messageContextAnchor = it },
                onReactionBurstRequested = { reactionBurst = it },
                onRetryMessage = { messageId ->
                    onUiEvent(GroupUiEvent.RetryMessage(messageId))
                },
                onSafetyWarningClick = { messageId, contactId, warning ->
                    onUiEvent(
                        GroupUiEvent.SafetyWarningClicked(
                            messageId = messageId,
                            contactId = contactId,
                            warning = warning
                        )
                    )
                },
                onAttachmentVisible = { attachmentId ->
                    onUiEvent(GroupUiEvent.AttachmentVisible(attachmentId))
                },
                onAttachmentClick = { messageId, attachmentId ->
                    viewerMessageId = messageId
                    viewerAttachmentId = attachmentId
                    onUiEvent(GroupUiEvent.AttachmentVisible(attachmentId))
                },
                onContactClick = { contact -> pendingSharedContact = contact }
            )
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
                    onUiEvent(GroupUiEvent.ShareContact(sharedContact))
                }
            },
            onBack = dismissOverlay,
            modifier = Modifier.fillMaxSize()
        )
    }

    pendingSharedContact?.let { contact ->
        AddSharedContactDialog(
            contact = contact,
            onConfirm = {
                pendingSharedContact = null
                onUiEvent(GroupUiEvent.AddSharedContact(contact))
            },
            onDismiss = { pendingSharedContact = null }
        )
    }

    val currentViewerMessage =
        viewerMessageId
            ?.let { messageId -> uiState.messages.firstOrNull { it.id == messageId } }
            ?.bubble
    val currentViewerAttachmentId = viewerAttachmentId
    if (currentViewerMessage != null && currentViewerAttachmentId != null) {
        MessageAttachmentViewer(
            attachments = currentViewerMessage.toMessageAttachmentsUi(),
            selectedAttachmentId = currentViewerAttachmentId,
            canSaveToCameraRoll = !currentViewerMessage.isMine,
            onDismiss = {
                viewerMessageId = null
                viewerAttachmentId = null
            },
            onEnsureAttachmentLoaded = { attachmentId ->
                onUiEvent(GroupUiEvent.AttachmentVisible(attachmentId))
            },
            onError = { error -> onUiEvent(GroupUiEvent.AttachmentError(error)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    uiState: GroupUiState,
    containerColor: Color,
    onUiEvent: (GroupUiEvent) -> Unit
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
                    modifier = Modifier.clickable { onUiEvent(GroupUiEvent.HeaderClicked) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SparrowAvatar(
                        name = uiState.title,
                        pictureBytes = uiState.avatarBytes,
                        size = Dimens.GroupScreen.topBarAvatarSize
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
                            text = subtitle(uiState),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { onUiEvent(GroupUiEvent.BackClicked) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        )

        uiState.errorMessage?.let { ErrorMessage(message = it) }
        StatusHint(uiState = uiState, onUiEvent = onUiEvent)
    }
}

/**
 * Tracks the "share current location" flow as a single state instead of three
 * independent booleans. IDLE -> CAPTURING (waiting on device location) ->
 * AWAITING_SEND (location obtained, event dispatched) -> SENDING (uiState confirmed
 * the send started) -> IDLE once uiState.isSending flips back to false.
 */
private enum class LocationSharePhase { IDLE, CAPTURING, AWAITING_SEND, SENDING }

@Composable
private fun BottomBar(
    uiState: GroupUiState,
    containerColor: Color,
    onUiEvent: (GroupUiEvent) -> Unit,
    onContactAttachmentClick: () -> Unit
) {
    var locationPhase by rememberSaveable { mutableStateOf(LocationSharePhase.IDLE) }

    LaunchedEffect(uiState.isSending, locationPhase) {
        locationPhase = when {
            locationPhase == LocationSharePhase.AWAITING_SEND && uiState.isSending ->
                LocationSharePhase.SENDING

            locationPhase == LocationSharePhase.SENDING && !uiState.isSending ->
                LocationSharePhase.IDLE

            else -> locationPhase
        }
    }

    val isLocationInProgress = locationPhase != LocationSharePhase.IDLE

    val currentLocationLauncher =
        rememberCurrentLocationLauncher(
            onLocation = { location ->
                locationPhase = LocationSharePhase.AWAITING_SEND
                onUiEvent(GroupUiEvent.ShareCurrentLocation(location))
            },
            onError = { error ->
                locationPhase = LocationSharePhase.IDLE
                onUiEvent(GroupUiEvent.AttachmentError(error))
            }
        )
    val maxAttachments = MessageAttachmentPolicy.MAX_ATTACHMENTS_PER_MESSAGE
    val mediaPicker =
        rememberMediaSelectionLauncher(
            maxItems = maxAttachments,
            maxImageDimension = MessageAttachmentPolicy.MAX_IMAGE_DIMENSION,
            maxImageBytes = MessageAttachmentPolicy.MAX_IMAGE_BYTES,
            maxVideoBytes = MessageAttachmentPolicy.MAX_VIDEO_BYTES,
            maxFileBytes = MessageAttachmentPolicy.MAX_FILE_BYTES,
            selectedMedia = uiState.selectedMedia,
            onResult = { result ->
                when (result) {
                    is MediaSelectionResult.Selected -> onUiEvent(GroupUiEvent.MediaSelected(result.media))
                    is MediaSelectionResult.Error -> onUiEvent(GroupUiEvent.AttachmentError(result.message))
                    MediaSelectionResult.Dismissed -> Unit
                }
            },
            onFilePickerSessionStarted = { sessionId ->
                onUiEvent(GroupUiEvent.OpenFilePicker(sessionId))
            }
        )
    val canAddAttachment =
        !uiState.isLoading &&
            !uiState.isSending &&
            !isLocationInProgress &&
            uiState.isMessageInputEnabled &&
            uiState.selectedMedia.size < maxAttachments

    MessageControl(
        containerColor = containerColor,
        state = MessageInputState(
            messageText = uiState.messageText,
            replyTo = uiState.replyTo,
            isTyping = uiState.isSomeoneTyping,
            contactName = uiState.typingDisplayName,
            isInputEnabled = !uiState.isLoading && !uiState.isSending && uiState.isMessageInputEnabled,
            isSendEnabled = !uiState.isLoading && !uiState.isSending && !isLocationInProgress && uiState.isMessageInputEnabled,
            isLocationInProgress = isLocationInProgress,
            selectedMedia = uiState.selectedMedia,
            isGalleryEnabled = canAddAttachment,
            isCameraEnabled = canAddAttachment,
            isFileEnabled = canAddAttachment
        ),
        actions = MessageInputActions(
            onValueChange = { onUiEvent(GroupUiEvent.MessageTextChanged(it)) },
            onSendClick = { onUiEvent(GroupUiEvent.SendClicked) },
            onCancelReply = { onUiEvent(GroupUiEvent.CancelReply) },
            onSelectionClick = mediaPicker::launch,
            onMediaRemove = { mediaId ->
                onUiEvent(
                    GroupUiEvent.MediaSelected(
                        uiState.selectedMedia.filterNot { it.id == mediaId }
                    )
                )
            },
            onClickGallery = { mediaPicker.launch(MediaSelectionSource.GALLERY) },
            onClickCamera = { mediaPicker.launch(MediaSelectionSource.CAMERA) },
            onClickFile = { mediaPicker.launch(MediaSelectionSource.FILE_PICKER) },
            onClickContact = onContactAttachmentClick,
            onClickLocation = {
                if (locationPhase == LocationSharePhase.IDLE) {
                    locationPhase = LocationSharePhase.CAPTURING
                    currentLocationLauncher.launch()
                }
            }
        )
    )
}

@Composable
private fun Content(
    uiState: GroupUiState,
    listState: LazyListState,
    innerPadding: PaddingValues,
    targetMessageId: String?,
    selectedContextMessageId: String?,
    onContextMessageRequested: (MessageContextAnchor) -> Unit,
    onReactionBurstRequested: (MessageReactionBurst) -> Unit,
    onRetryMessage: (String) -> Unit,
    onSafetyWarningClick: (String, String?, MessageSafetyWarningUi) -> Unit,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String, String) -> Unit,
    onContactClick: (SharedContact) -> Unit
) {
    val fillModifier = Modifier.fillMaxSize().padding(innerPadding)
    val messageState =
        rememberDissolvingMessageListState(
            messages = uiState.messages,
            idOf = GroupMessageUi::id
        )

    when {
        uiState.isLoading -> LoadingContent(modifier = fillModifier)

        messageState.messages.isEmpty() -> EmptyContent(
            title = uiState.title,
            modifier = fillModifier
        )

        else -> MessageList(
            messageState = messageState,
            listState = listState,
            targetMessageId = targetMessageId,
            selectedContextMessageId = selectedContextMessageId,
            onContextMessageRequested = onContextMessageRequested,
            onReactionBurstRequested = onReactionBurstRequested,
            onRetryMessage = onRetryMessage,
            onSafetyWarningClick = onSafetyWarningClick,
            onAttachmentVisible = onAttachmentVisible,
            onAttachmentClick = onAttachmentClick,
            onContactClick = onContactClick,
            contentPadding = innerPadding
        )
    }
}

@Composable
private fun MessageList(
    messageState: DissolvingMessageListState<GroupMessageUi>,
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
    val messages = messageState.messages
    val searchTargetState =
        rememberMessageSearchTargetState(
            targetMessageId = targetMessageId,
            messageIds = messages.map(GroupMessageUi::id),
            listState = listState
        )
    val replyJumpState =
        rememberMessageJumpState(
            messageIds = messages.map(GroupMessageUi::id),
            listState = listState
        )
    val newestMessage = messages.firstOrNull()
    LaunchedEffect(newestMessage?.id) {
        if (searchTargetState.isHandled && newestMessage?.bubble?.isMine == true) {
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
        items(items = messages, key = GroupMessageUi::id) { message ->
            MessageDissolve(
                isDissolving = messageState.isDissolving(message),
                onFinished = { messageState.finishDissolve(message.id) }
            ) {
                if (message.type == ChatMessageType.USER) {
                    GroupMessageBubble(
                        message = message,
                        onRetryMessage = onRetryMessage,
                        onSafetyWarningClick = onSafetyWarningClick,
                        onAttachmentVisible = onAttachmentVisible,
                        onAttachmentClick = onAttachmentClick,
                        onContactClick = onContactClick,
                        onReplyPreviewClick = replyJumpState.jumpTo,
                        onContextMessageRequested = onContextMessageRequested,
                        onReactionBurstRequested = onReactionBurstRequested,
                        isContextSelected = selectedContextMessageId == message.id,
                        isSearchHighlighted =
                            message.id == searchTargetState.highlightedMessageId ||
                                message.id == replyJumpState.highlightedMessageId
                    )
                } else {
                    MembershipSystemMessage(
                        type = message.type,
                        memberName = message.bubble.senderName
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupMessageBubble(
    message: GroupMessageUi,
    onRetryMessage: (String) -> Unit,
    onSafetyWarningClick: (String, String?, MessageSafetyWarningUi) -> Unit,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String, String) -> Unit,
    onContactClick: (SharedContact) -> Unit,
    onReplyPreviewClick: (String) -> Unit,
    onContextMessageRequested: (MessageContextAnchor) -> Unit,
    onReactionBurstRequested: (MessageReactionBurst) -> Unit,
    isContextSelected: Boolean,
    modifier: Modifier = Modifier,
    isSearchHighlighted: Boolean = false,
    showMetadata: Boolean = true,
    contextMenuEnabled: Boolean = true
) {
    var anchor by remember(message.id) { mutableStateOf<MessageContextAnchor?>(null) }

    val contextModifier =
        Modifier
            .captureMessageContextAnchor(
                messageId = message.id,
                isMine = message.bubble.isMine,
                onAnchorChanged = { anchor = it }
            )
            .alpha(if (isContextSelected) 0f else 1f)

    val onActionMenuVisibilityChange: (Boolean) -> Unit = { isVisible ->
        if (contextMenuEnabled && isVisible) {
            anchor?.let(onContextMessageRequested)
        }
    }

    if (message.bubble.isMine) {
        MessageBubble(
            message = message.bubble,
            onRetryClick = { onRetryMessage(message.id) },
            onSafetyDetailsClick = { warning ->
                onSafetyWarningClick(message.id, message.senderContactId, warning)
            },
            onAttachmentVisible = onAttachmentVisible,
            onAttachmentClick = { attachmentId -> onAttachmentClick(message.id, attachmentId) },
            onContactClick = onContactClick,
            onReplyPreviewClick = onReplyPreviewClick,
            onActionMenuVisibilityChange = onActionMenuVisibilityChange,
            onReactionsClick = { boundsInRoot ->
                onReactionBurstRequested(
                    MessageReactionBurst(
                        reactions = message.bubble.reactions,
                        boundsInRoot = boundsInRoot
                    )
                )
            },
            modifier = modifier.then(contextModifier),
            isSearchHighlighted = isSearchHighlighted,
            showMetadata = showMetadata
        )
        return
    }

    Row(
        modifier = modifier.fillMaxWidth().then(contextModifier),
        verticalAlignment = Alignment.Bottom
    ) {
        SparrowAvatar(
            name = message.bubble.senderName.orEmpty(),
            pictureBytes = message.senderProfilePictureBytes,
            size = Dimens.GroupScreen.typingAvatarSize
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.groupScreen.senderGap))
        MessageBubble(
            message = message.bubble,
            onRetryClick = { onRetryMessage(message.id) },
            onSafetyDetailsClick = { warning ->
                onSafetyWarningClick(message.id, message.senderContactId, warning)
            },
            onAttachmentVisible = onAttachmentVisible,
            onAttachmentClick = { attachmentId -> onAttachmentClick(message.id, attachmentId) },
            onContactClick = onContactClick,
            onReplyPreviewClick = onReplyPreviewClick,
            onActionMenuVisibilityChange = onActionMenuVisibilityChange,
            onReactionsClick = { boundsInRoot ->
                onReactionBurstRequested(
                    MessageReactionBurst(
                        reactions = message.bubble.reactions,
                        boundsInRoot = boundsInRoot
                    )
                )
            },
            modifier = Modifier.weight(1f),
            isSearchHighlighted = isSearchHighlighted,
            showMetadata = showMetadata
        )
    }
}

@Composable
private fun StatusHint(
    uiState: GroupUiState,
    onUiEvent: (GroupUiEvent) -> Unit
) {
    when {
        uiState.showInvitationActions ->
            InvitationHint(
                onAccept = { onUiEvent(GroupUiEvent.AcceptInvitation) },
                onDecline = { onUiEvent(GroupUiEvent.DeclineInvitation) }
            )

        uiState.state == GroupConversationState.DELETED -> ConversationDeletedHint()
        uiState.state == GroupConversationState.REMOVED ||
            (uiState.state == GroupConversationState.DECLINED && uiState.messages.isNotEmpty()) ->
            MembershipRemovedHint()

        uiState.state == GroupConversationState.LEAVING -> MembershipLeavingHint()
        uiState.state != GroupConversationState.READY && uiState.isMessageInputEnabled ->
            PendingMessageHint(uiState = uiState)
    }
}

@Composable
private fun PendingMessageHint(uiState: GroupUiState) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.medium,
                        vertical = MaterialTheme.spacing.small
                    )
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_message_queued),
                style = MaterialTheme.typography.bodySmall
            )
            uiState.memberProgress.forEach { member ->
                Text(
                    text = "${member.displayName} · ${memberStatus(member.status)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun subtitle(uiState: GroupUiState): String =
    when (uiState.state) {
        GroupConversationState.READY ->
            stringResource(Res.string.feature_chats_group_member_count, uiState.memberCount)

        GroupConversationState.INVITED -> stringResource(Res.string.feature_chats_group_status_invited)
        GroupConversationState.JOINING -> stringResource(Res.string.feature_chats_group_status_joining)
        GroupConversationState.WAITING_FOR_MEMBERS ->
            pendingSubtitle(
                readyCount = uiState.readyMemberCount,
                pendingCount = uiState.pendingMemberCount,
                waitingResource = Res.string.feature_chats_group_status_waiting
            )

        GroupConversationState.DISTRIBUTING_KEYS ->
            pendingSubtitle(
                readyCount = uiState.readyMemberCount,
                pendingCount = uiState.pendingMemberCount,
                waitingResource = Res.string.feature_chats_group_status_distributing
            )

        GroupConversationState.LEAVING -> stringResource(Res.string.feature_chats_group_status_leaving)
        GroupConversationState.REMOVED -> stringResource(Res.string.feature_chats_group_status_removed)
        GroupConversationState.DELETED -> stringResource(Res.string.feature_chats_group_deleted_status)
        GroupConversationState.DECLINED -> stringResource(Res.string.feature_chats_group_status_declined)
        GroupConversationState.EXPIRED -> stringResource(Res.string.feature_chats_group_status_expired)
        GroupConversationState.FAILED -> stringResource(Res.string.feature_chats_group_status_failed)
    }

@Composable
private fun pendingSubtitle(
    readyCount: Int,
    pendingCount: Int,
    waitingResource: StringResource
): String =
    if (readyCount > 0) {
        stringResource(
            Res.string.feature_chats_group_status_partial,
            readyCount,
            pendingCount
        )
    } else {
        stringResource(waitingResource, pendingCount)
    }

@Composable
private fun memberStatus(status: GroupMemberInvitationStatus): String =
    when (status) {
        GroupMemberInvitationStatus.INVITED -> stringResource(Res.string.feature_chats_group_member_invited)
        GroupMemberInvitationStatus.ACCEPTED -> stringResource(Res.string.feature_chats_group_member_accepted)
        GroupMemberInvitationStatus.KEY_SENT -> stringResource(Res.string.feature_chats_group_member_key_sent)
        GroupMemberInvitationStatus.ACTIVE -> stringResource(Res.string.feature_chats_group_member_active)
        GroupMemberInvitationStatus.DECLINED -> stringResource(Res.string.feature_chats_group_member_declined)
        GroupMemberInvitationStatus.EXPIRED -> stringResource(Res.string.feature_chats_group_member_expired)
        GroupMemberInvitationStatus.FAILED -> stringResource(Res.string.feature_chats_group_member_failed)
    }

@Composable
private fun EmptyContent(
    title: String,
    modifier: Modifier = Modifier
) {
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

@Composable
private fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.small,
                    vertical = MaterialTheme.spacing.base
                ),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ConversationDeletedHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.screenPadding,
                vertical = MaterialTheme.spacing.small
            )
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_deleted_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_deleted_description),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
private fun ConversationDeletedHintPreview() {
    SparrowTheme {
        ConversationDeletedHint()
    }
}

@Composable
private fun InvitationHint(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.screenPadding,
                        vertical = MaterialTheme.spacing.small
                    )
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_invitation_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_invitation_description),
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.base),
                horizontalArrangement = Arrangement.End
            ) {
                SparrowBannerButton(
                    onClick = onDecline,
                    fillMaxWidth = false,
                    text = stringResource(Res.string.feature_chats_group_decline)
                )

                SparrowBannerButton(
                    onClick = onAccept,
                    fillMaxWidth = false,
                    text = stringResource(Res.string.feature_chats_group_accept)
                )
            }
        }
    }
}

@Preview
@Composable
private fun InvitationHintPreview() {
    SparrowTheme {
        InvitationHint(
            onAccept = {},
            onDecline = {}
        )
    }
}

@Composable
private fun MembershipLeavingHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.screenPadding,
                vertical = MaterialTheme.spacing.small
            )
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_leaving_hint_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_leaving_hint_description),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
private fun MembershipLeavingHintPreview() {
    SparrowTheme {
        MembershipLeavingHint()
    }
}

@Composable
private fun MembershipRemovedHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.screenPadding,
                vertical = MaterialTheme.spacing.small
            )
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_removed_hint_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_removed_hint_description),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
private fun MembershipRemovedHintPreview() {
    SparrowTheme {
        MembershipRemovedHint()
    }
}

@Composable
private fun MembershipSystemMessage(
    type: ChatMessageType,
    memberName: String?,
    modifier: Modifier = Modifier
) {
    val text = getSystemMessage(type, memberName)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = Alpha.GroupScreen.membershipSystemMessage),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier =
                    Modifier.padding(
                        horizontal = MaterialTheme.spacing.base,
                        vertical = MaterialTheme.spacing.base
                    ),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        if (type == ChatMessageType.GROUP_MEMBER_ADDED) {
                            Icons.Default.PersonAdd
                        } else {
                            Icons.Default.PersonRemove
                        },
                    modifier = Modifier.size(Dimens.GroupScreen.noticeIconSize),
                    contentDescription = null
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun resolvedMemberName(memberName: String?): String =
    memberName?.takeIf(String::isNotBlank) ?: stringResource(Res.string.feature_chats_group_unknown_member)

@Composable
private fun getSystemMessage(
    type: ChatMessageType,
    memberName: String?
) = when (type) {
    ChatMessageType.GROUP_MEMBER_ADDED ->
        stringResource(Res.string.feature_chats_group_member_added_message, resolvedMemberName(memberName))

    ChatMessageType.GROUP_MEMBER_REMOVED ->
        stringResource(Res.string.feature_chats_group_member_removed_message, resolvedMemberName(memberName))

    ChatMessageType.LOCAL_GROUP_MEMBERSHIP_REMOVED ->
        stringResource(Res.string.feature_chats_group_you_were_removed_message)

    ChatMessageType.GROUP_MEMBER_LEFT ->
        stringResource(Res.string.feature_chats_group_member_left_message, resolvedMemberName(memberName))

    ChatMessageType.LOCAL_GROUP_MEMBERSHIP_LEFT ->
        stringResource(Res.string.feature_chats_group_you_left_message)

    ChatMessageType.USER -> ""
}

@Preview
@Composable
private fun MembershipSystemMessageAddedPreview() {
    SparrowTheme {
        MembershipSystemMessage(
            type = ChatMessageType.GROUP_MEMBER_ADDED,
            memberName = "Alex"
        )
    }
}

@Preview
@Composable
private fun MembershipSystemMessagePreview() {
    SparrowTheme {
        MembershipSystemMessage(
            type = ChatMessageType.GROUP_MEMBER_REMOVED,
            memberName = "Alex"
        )
    }
}

@Preview
@Composable
private fun MembershipSystemMessageLeftPreview() {
    SparrowTheme {
        MembershipSystemMessage(
            type = ChatMessageType.GROUP_MEMBER_LEFT,
            memberName = "Alex"
        )
    }
}

@Preview
@Composable
private fun GroupScreenPreview() {
    SparrowTheme {
        GroupScreen(
            uiState =
                GroupUiState(
                    title = "Family",
                    isLoading = false,
                    isMessageInputEnabled = true,
                    memberCount = 4
                ),
            onUiEvent = {}
        )
    }
}

@Preview
@Composable
private fun GroupMessagesPreview() {
    SparrowTheme {
        GroupScreen(
            uiState =
                GroupUiState(
                    title = "Family",
                    isLoading = false,
                    isMessageInputEnabled = true,
                    memberCount = 4,
                    messages =
                        listOf(
                            GroupMessageUi(
                                bubble =
                                    MessageBubbleUi(
                                        id = "1",
                                        isMine = false,
                                        security = MessageSecurity.END_TO_END_ENCRYPTED,
                                        contentStatus = MessageContentStatus.READABLE,
                                        deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE,
                                        senderName = "Alex",
                                        senderIsInContacts = true,
                                        textPart =
                                            MessagePartUi.Text(
                                                text = "Hello everyone",
                                                isContentFailed = false
                                            )
                                    ),
                                type = ChatMessageType.USER
                            )
                        )
                ),
            onUiEvent = {}
        )
    }
}
