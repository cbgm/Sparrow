package com.cbgm.sparrow.feature.chats.presentation.direct.screen

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.ui.component.PatternBackground
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowAvatar
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.component.SparrowOverlayHost
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
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
import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
import com.cbgm.sparrow.feature.chats.presentation.component.AddSharedContactDialog
import com.cbgm.sparrow.feature.chats.presentation.component.MessageBubble
import com.cbgm.sparrow.feature.chats.presentation.component.MessageContextAnchor
import com.cbgm.sparrow.feature.chats.presentation.component.MessageContextHost
import com.cbgm.sparrow.feature.chats.presentation.component.MessageControl
import com.cbgm.sparrow.feature.chats.presentation.component.MessageInputActions
import com.cbgm.sparrow.feature.chats.presentation.component.MessageInputState
import com.cbgm.sparrow.feature.chats.presentation.component.captureMessageContextAnchor
import com.cbgm.sparrow.feature.chats.presentation.component.mapper.toMessageAttachmentsUi
import com.cbgm.sparrow.feature.chats.presentation.component.mapper.toSharedContact
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.rememberMessageJumpState
import com.cbgm.sparrow.feature.chats.presentation.component.rememberMessageSearchTargetState
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectComposerState
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectUiEvent
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectUiState
import com.cbgm.sparrow.feature.contacts.presentation.overview.ContactAttachmentSelectionRoute
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResult
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.feature.media.presentation.selection.rememberMediaSelectionLauncher
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.base_verify
import com.cbgm.sparrow.resources.feature_chats_chat_key_exchange_incomplete_description
import com.cbgm.sparrow.resources.feature_chats_chat_key_exchange_incomplete_title
import com.cbgm.sparrow.resources.feature_chats_chat_no_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_one_way_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_unencrypted_description
import com.cbgm.sparrow.resources.feature_chats_chat_unencrypted_title
import com.cbgm.sparrow.resources.feature_chats_chat_unverified_description
import com.cbgm.sparrow.resources.feature_chats_chat_unverified_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_unverified_title
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_contact_description
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_contact_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_contact_title
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_me_description
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_me_keys_description
import com.cbgm.sparrow.resources.feature_chats_chat_verified_by_me_title
import com.cbgm.sparrow.resources.feature_chats_chat_verified_e2ee
import com.cbgm.sparrow.resources.feature_chats_chat_verified_keys_description
import com.cbgm.sparrow.resources.feature_chats_import_contact_identity
import com.cbgm.sparrow.resources.feature_chats_loading_chat
import com.cbgm.sparrow.resources.feature_chats_manual_identity_incomplete_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_incomplete_title
import com.cbgm.sparrow.resources.feature_chats_manual_identity_required_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_required_title
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_action
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_description
import com.cbgm.sparrow.resources.feature_chats_manual_identity_setup_title
import com.cbgm.sparrow.resources.feature_chats_start_conversation_with
import com.cbgm.sparrow.resources.feature_identity_share_my_identity
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectScreen(
    uiState: DirectUiState,
    onUiEvent: (DirectUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    targetMessageId: String? = null
) {
    var showIdentitySetupDialog by rememberSaveable { mutableStateOf(false) }
    var viewerMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    var viewerAttachmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var showContactSelection by rememberSaveable { mutableStateOf(false) }
    var pendingSharedContact by remember { mutableStateOf<SharedContact?>(null) }
    var messageContextAnchor by remember { mutableStateOf<MessageContextAnchor?>(null) }

    val contextMessage =
        messageContextAnchor
            ?.messageId
            ?.let { messageId -> uiState.messages.firstOrNull { it.id == messageId } }

    val activeContextAnchor =
        messageContextAnchor?.takeIf {
            contextMessage != null
        }

    MessageContextHost(
        anchor = activeContextAnchor,
        menuColor =
            if (contextMessage?.isMine == true) {
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
                    DirectUiEvent.ReplyToMessage(
                        message.id
                    )
                )
            }
        },
        modifier = modifier,
        preview = {
            contextMessage?.let { message ->
                MessageBubble(
                    message = message,
                    onRetryClick = {},
                    onSafetyDetailsClick = {},
                    onAttachmentVisible = {},
                    onAttachmentClick = {},
                    onContactClick = {},
                    onReplyPreviewClick = {},
                    isSearchHighlighted = false,
                    showMetadata = false
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
                    onUiEvent = onUiEvent,
                    onManualIdentitySetup = { showIdentitySetupDialog = true }
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
                onRetryMessage = { messageId ->
                    onUiEvent(DirectUiEvent.RetryMessage(messageId))
                },
                onSafetyWarningClick = { messageId, warning ->
                    onUiEvent(
                        DirectUiEvent.SafetyWarningClicked(
                            messageId = messageId,
                            warning = warning
                        )
                    )
                },
                onAttachmentVisible = { attachmentId ->
                    onUiEvent(DirectUiEvent.AttachmentVisible(attachmentId))
                },
                onAttachmentClick = { messageId, attachmentId ->
                    viewerMessageId = messageId
                    viewerAttachmentId = attachmentId
                    onUiEvent(DirectUiEvent.AttachmentVisible(attachmentId))
                },
                onContactClick = { contact -> pendingSharedContact = contact }
            )
        }
    }

    if (showIdentitySetupDialog) {
        IdentitySetupDialog(
            onShareIdentity = {
                showIdentitySetupDialog = false
                onUiEvent(DirectUiEvent.ShareIdentityClicked)
            },
            onImportIdentity = {
                showIdentitySetupDialog = false
                onUiEvent(DirectUiEvent.ImportIdentityClicked)
            },
            onDismiss = { showIdentitySetupDialog = false }
        )
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
                    onUiEvent(DirectUiEvent.ShareContact(sharedContact))
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
                onUiEvent(DirectUiEvent.AddSharedContact(contact))
            },
            onDismiss = { pendingSharedContact = null }
        )
    }

    val currentViewerMessage =
        viewerMessageId?.let { messageId -> uiState.messages.firstOrNull { it.id == messageId } }
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
                onUiEvent(DirectUiEvent.AttachmentVisible(attachmentId))
            },
            onError = { error -> onUiEvent(DirectUiEvent.AttachmentError(error)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    uiState: DirectUiState,
    containerColor: Color,
    onUiEvent: (DirectUiEvent) -> Unit,
    onManualIdentitySetup: () -> Unit
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
                    modifier = Modifier.clickable { onUiEvent(DirectUiEvent.HeaderClicked) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SparrowAvatar(
                        name = uiState.contactName,
                        pictureBytes = uiState.profilePictureBytes,
                        size = Dimens.DirectScreen.topBarAvatarSize
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Text(
                        text = uiState.contactName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { onUiEvent(DirectUiEvent.BackClicked) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        )

        if (!uiState.isLoading) {
            SecurityBanner(
                securityState = uiState.contactSecurityState,
                identitySetupMode = uiState.identitySetupMode,
                isChatAuthorized = uiState.isChatAuthorized,
                onVerifyIdentity = { onUiEvent(DirectUiEvent.VerifyIdentityClicked) },
                onManualIdentitySetup = onManualIdentitySetup
            )
        }

        uiState.errorMessage?.let { message -> ErrorMessage(message = message) }
    }
}

@Composable
private fun IdentitySetupDialog(
    onShareIdentity: () -> Unit,
    onImportIdentity: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_chats_manual_identity_setup_title),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(Res.string.feature_chats_manual_identity_setup_description))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                SparrowOutlinedButton(
                    onClick = onShareIdentity,
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.feature_identity_share_my_identity)
                )
                SparrowOutlinedButton(
                    onClick = onImportIdentity,
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.feature_chats_import_contact_identity)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            SparrowSecondaryButton(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}

@Preview
@Composable
private fun IdentitySetupDialogPreview() {
    SparrowTheme {
        IdentitySetupDialog(
            onShareIdentity = {},
            onImportIdentity = {},
            onDismiss = {}
        )
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
    uiState: DirectUiState,
    containerColor: Color,
    onUiEvent: (DirectUiEvent) -> Unit,
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
                onUiEvent(DirectUiEvent.ShareCurrentLocation(location))
            },
            onError = { error ->
                locationPhase = LocationSharePhase.IDLE
                onUiEvent(DirectUiEvent.AttachmentError(error))
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
                    is MediaSelectionResult.Selected -> onUiEvent(DirectUiEvent.MediaSelected(result.media))
                    is MediaSelectionResult.Error -> onUiEvent(DirectUiEvent.AttachmentError(result.message))
                    MediaSelectionResult.Dismissed -> Unit
                }
            },
            onFilePickerSessionStarted = { sessionId ->
                onUiEvent(DirectUiEvent.OpenFilePicker(sessionId))
            }
        )
    val canAddAttachment =
        !uiState.isLoading &&
            !uiState.isSending &&
            !isLocationInProgress &&
            uiState.composerState.isInputEnabled &&
            uiState.selectedMedia.size < maxAttachments

    MessageControl(
        containerColor = containerColor,
        state = MessageInputState(
            messageText = uiState.messageText,
            replyTo = uiState.replyTo,
            isTyping = uiState.isContactTyping,
            contactName = uiState.contactName,
            isInputEnabled = !uiState.isLoading && !uiState.isSending && uiState.composerState.isInputEnabled,
            isSendEnabled = !uiState.isLoading && !uiState.isSending && !isLocationInProgress && uiState.composerState.isSendActionEnabled,
            isLocationInProgress = isLocationInProgress,
            selectedMedia = uiState.selectedMedia,
            isGalleryEnabled = canAddAttachment,
            isCameraEnabled = canAddAttachment,
            isFileEnabled = canAddAttachment
        ),
        actions = MessageInputActions(
            onValueChange = { onUiEvent(DirectUiEvent.MessageTextChanged(it)) },
            onSendClick = { onUiEvent(DirectUiEvent.SendClicked) },
            onCancelReply = { onUiEvent(DirectUiEvent.CancelReply) },
            onSelectionClick = mediaPicker::launch,
            onMediaRemove = { mediaId ->
                onUiEvent(
                    DirectUiEvent.MediaSelected(
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
    uiState: DirectUiState,
    listState: LazyListState,
    innerPadding: PaddingValues,
    targetMessageId: String?,
    selectedContextMessageId: String?,
    onContextMessageRequested: (MessageContextAnchor) -> Unit,
    onRetryMessage: (String) -> Unit,
    onSafetyWarningClick: (String, MessageSafetyWarningUi) -> Unit,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String, String) -> Unit,
    onContactClick: (SharedContact) -> Unit
) {
    val fillModifier = Modifier.fillMaxSize().padding(innerPadding)

    when {
        uiState.isLoading -> LoadingContent(modifier = fillModifier)

        uiState.messages.isEmpty() -> EmptyContent(
            contactName = uiState.contactName,
            securityState = uiState.contactSecurityState,
            modifier = fillModifier
        )

        else -> MessageList(
            messages = uiState.messages,
            listState = listState,
            targetMessageId = targetMessageId,
            selectedContextMessageId = selectedContextMessageId,
            onContextMessageRequested = onContextMessageRequested,
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
    messages: List<MessageBubbleUi>,
    listState: LazyListState,
    targetMessageId: String?,
    selectedContextMessageId: String?,
    onContextMessageRequested: (MessageContextAnchor) -> Unit,
    onRetryMessage: (String) -> Unit,
    onSafetyWarningClick: (String, MessageSafetyWarningUi) -> Unit,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String, String) -> Unit,
    onContactClick: (SharedContact) -> Unit,
    contentPadding: PaddingValues
) {
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
            var anchor by remember(message.id) { mutableStateOf<MessageContextAnchor?>(null) }
            val isContextSelected = selectedContextMessageId == message.id

            MessageBubble(
                message = message,
                onRetryClick = { onRetryMessage(message.id) },
                onSafetyDetailsClick = { warning ->
                    onSafetyWarningClick(message.id, warning)
                },
                onAttachmentVisible = onAttachmentVisible,
                onAttachmentClick = { attachmentId -> onAttachmentClick(message.id, attachmentId) },
                onContactClick = onContactClick,
                onReplyPreviewClick = replyJumpState.jumpTo,
                onActionMenuVisibilityChange = { isVisible ->
                    if (isVisible) {
                        anchor?.let(onContextMessageRequested)
                    }
                },
                modifier = Modifier
                    .captureMessageContextAnchor(
                        messageId = message.id,
                        isMine = message.isMine,
                        onAnchorChanged = { anchor = it }
                    )
                    .alpha(if (isContextSelected) 0f else 1f),
                isSearchHighlighted =
                    message.id == searchTargetState.highlightedMessageId ||
                        message.id == replyJumpState.highlightedMessageId
            )
        }
    }
}

@Composable
private fun EmptyContent(
    contactName: String,
    securityState: ContactSecurityState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(
                    Res.string.feature_chats_start_conversation_with,
                    contactName
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = securityDescription(securityState),
                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun securityDescription(securityState: ContactSecurityState): String =
    when (securityState) {
        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS -> stringResource(Res.string.feature_chats_chat_no_keys_description)
        ContactSecurityState.ONE_WAY_KEYS -> stringResource(Res.string.feature_chats_chat_one_way_keys_description)
        ContactSecurityState.MUTUAL_KEYS_UNVERIFIED -> stringResource(Res.string.feature_chats_chat_unverified_keys_description)
        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME -> stringResource(Res.string.feature_chats_chat_verified_by_me_keys_description)
        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT -> stringResource(Res.string.feature_chats_chat_verified_by_contact_keys_description)
        ContactSecurityState.MUTUAL_KEYS_VERIFIED -> stringResource(Res.string.feature_chats_chat_verified_keys_description)
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
        modifier = modifier
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
private fun SecurityBanner(
    securityState: ContactSecurityState,
    identitySetupMode: DirectIdentitySetupMode,
    isChatAuthorized: Boolean,
    onVerifyIdentity: () -> Unit,
    onManualIdentitySetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (securityState == ContactSecurityState.MUTUAL_KEYS_VERIFIED && isChatAuthorized) {
        VerifiedSecurityIndicator(modifier = modifier)
        return
    }

    val state =
        securityState(
            securityState = securityState,
            identitySetupMode = identitySetupMode
        ) ?: return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = state.containerColor,
        contentColor = state.contentColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.directScreen.securityBannerVerticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = state.icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.DirectScreen.invitationIconSize)
            )
            Column(
                modifier = Modifier.padding(start = MaterialTheme.spacing.small).weight(1f)
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.description,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.directScreen.securityDescriptionTopPadding),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            SecurityAction(
                securityState = securityState,
                identitySetupMode = identitySetupMode,
                contentColor = state.contentColor,
                onVerifyIdentity = onVerifyIdentity,
                onManualIdentitySetup = onManualIdentitySetup
            )
        }
    }
}

@Composable
private fun SecurityAction(
    securityState: ContactSecurityState,
    identitySetupMode: DirectIdentitySetupMode,
    contentColor: Color,
    onVerifyIdentity: () -> Unit,
    onManualIdentitySetup: () -> Unit
) {
    when {
        identitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING &&
            securityState in setOf(
                ContactSecurityState.NO_REMOTE_PUBLIC_KEYS,
                ContactSecurityState.ONE_WAY_KEYS
            ) -> {
            TextButton(onClick = onManualIdentitySetup) {
                Text(
                    text = stringResource(Res.string.feature_chats_manual_identity_setup_action),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        securityState == ContactSecurityState.MUTUAL_KEYS_UNVERIFIED ||
            securityState == ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT -> {
            TextButton(onClick = onVerifyIdentity) {
                Text(
                    text = stringResource(Res.string.base_verify),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun securityState(
    securityState: ContactSecurityState,
    identitySetupMode: DirectIdentitySetupMode
): SecurityBannerState? {
    val isManualSetup = identitySetupMode == DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING

    return when (securityState) {
        ContactSecurityState.NO_REMOTE_PUBLIC_KEYS ->
            errorBanner(
                icon = Icons.Default.LockOpen,
                title = stringResource(
                    if (isManualSetup) {
                        Res.string.feature_chats_manual_identity_required_title
                    } else {
                        Res.string.feature_chats_chat_unencrypted_title
                    }
                ),
                description = stringResource(
                    if (isManualSetup) {
                        Res.string.feature_chats_manual_identity_required_description
                    } else {
                        Res.string.feature_chats_chat_unencrypted_description
                    }
                )
            )

        ContactSecurityState.ONE_WAY_KEYS ->
            errorBanner(
                icon = Icons.Default.LockOpen,
                title = stringResource(
                    if (isManualSetup) {
                        Res.string.feature_chats_manual_identity_incomplete_title
                    } else {
                        Res.string.feature_chats_chat_key_exchange_incomplete_title
                    }
                ),
                description = stringResource(
                    if (isManualSetup) {
                        Res.string.feature_chats_manual_identity_incomplete_description
                    } else {
                        Res.string.feature_chats_chat_key_exchange_incomplete_description
                    }
                )
            )

        ContactSecurityState.MUTUAL_KEYS_UNVERIFIED ->
            errorBanner(
                title = stringResource(Res.string.feature_chats_chat_unverified_title),
                description = stringResource(Res.string.feature_chats_chat_unverified_description)
            )

        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME ->
            SecurityBannerState(
                icon = Icons.Default.Schedule,
                title = stringResource(Res.string.feature_chats_chat_verified_by_me_title),
                description = stringResource(Res.string.feature_chats_chat_verified_by_me_description),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

        ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT ->
            SecurityBannerState(
                icon = Icons.Default.Security,
                title = stringResource(Res.string.feature_chats_chat_verified_by_contact_title),
                description = stringResource(Res.string.feature_chats_chat_verified_by_contact_description),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

        ContactSecurityState.MUTUAL_KEYS_VERIFIED -> null
    }
}

@Composable
private fun errorBanner(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Warning
) =
    SecurityBannerState(
        icon = icon,
        title = title,
        description = description,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    )

@Composable
private fun VerifiedSecurityIndicator(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = Alpha.DirectScreen.securityBanner),
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.directScreen.verifiedBannerVerticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(Dimens.DirectScreen.statusIconSize),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.directScreen.verifiedContentGap))
            Text(
                text = stringResource(Res.string.feature_chats_chat_verified_e2ee),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

private data class SecurityBannerState(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val containerColor: Color,
    val contentColor: Color
)

@Preview
@Composable
private fun DirectScreenPreview() {
    SparrowTheme {
        DirectScreen(
            uiState =
                DirectUiState(
                    contactName = "Alex",
                    isLoading = false,
                    composerState = DirectComposerState.READY,
                    contactSecurityState = ContactSecurityState.MUTUAL_KEYS_VERIFIED
                ),
            onUiEvent = {}
        )
    }
}

@Preview
@Composable
private fun DirectMessagesPreview() {
    SparrowTheme {
        DirectScreen(
            uiState =
                DirectUiState(
                    contactName = "Alex",
                    isLoading = false,
                    composerState = DirectComposerState.READY,
                    contactSecurityState = ContactSecurityState.MUTUAL_KEYS_VERIFIED,
                    messages =
                        listOf(
                            MessageBubbleUi(
                                id = "1",
                                isMine = true,
                                security = MessageSecurity.END_TO_END_ENCRYPTED,
                                contentStatus = MessageContentStatus.READABLE,
                                deliveryStatus = MessageDeliveryStatus.DELIVERED
                            )
                        )
                ),
            onUiEvent = {}
        )
    }
}
