package com.cbgm.sparrow.feature.chats.presentation.direct

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.cbgm.sparrow.core.ui.component.SparrowAvatar
import com.cbgm.sparrow.core.ui.component.SparrowLazyScaffold
import com.cbgm.sparrow.core.ui.component.SparrowOverlay
import com.cbgm.sparrow.core.ui.component.SparrowOverlayHost
import com.cbgm.sparrow.core.ui.device.clipboard.rememberClipboardWriter
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.presentation.component.MessageAttachmentViewer
import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
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
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageContextUiState
import com.cbgm.sparrow.feature.chats.presentation.component.model.TypingUiState
import com.cbgm.sparrow.feature.chats.presentation.component.rememberDissolvingMessageListState
import com.cbgm.sparrow.feature.chats.presentation.direct.component.ErrorMessage
import com.cbgm.sparrow.feature.chats.presentation.direct.component.IdentitySetupDialog
import com.cbgm.sparrow.feature.chats.presentation.direct.component.SecurityBanner
import com.cbgm.sparrow.feature.chats.presentation.direct.component.securityDescription
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiEvent
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiState
import com.cbgm.sparrow.feature.contacts.presentation.overview.ContactAttachmentSelectionRoute
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.common_copied
import com.cbgm.sparrow.resources.feature_chats_loading_chat
import com.cbgm.sparrow.resources.feature_chats_start_conversation_with
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectConversationScreen(
    uiState: DirectConversationUiState,
    composerState: MessageComposerUiState,
    contextState: MessageContextUiState<MessageBubbleUi>,
    typingState: TypingUiState,
    errorMessage: String?,
    onUiEvent: (DirectConversationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    targetMessageId: String? = null
) {
    var showIdentitySetupDialog by rememberSaveable { mutableStateOf(false) }
    var viewerMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    var viewerAttachmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var showContactSelection by rememberSaveable { mutableStateOf(false) }
    var pendingSharedContact by remember { mutableStateOf<SharedContact?>(null) }
    var messageContextAnchor by remember { mutableStateOf<MessageContextAnchor?>(null) }
    var reactionBurst by remember { mutableStateOf<MessageReactionBurst?>(null) }
    var feedbackOverlay by remember { mutableStateOf<FeedbackOverlayData?>(null) }

    val clipboardWriter = rememberClipboardWriter()
    val copiedText = stringResource(Res.string.common_copied)
    val contextMenuColor =
        if (contextState.message?.isMine == true) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }

    val activeContextAnchor =
        messageContextAnchor?.takeIf { anchor ->
            anchor.messageId == contextState.message?.id
        }

    Box(modifier = modifier.fillMaxSize()) {
        MessageContextHost(
            anchor = activeContextAnchor,
            menuColor = contextMenuColor,
            onDismiss = {
                messageContextAnchor = null
                onUiEvent(DirectConversationUiEvent.MessageContextDismissed)
            },
            onReplyClick = {
                contextState.message?.id?.let { messageId ->
                    onUiEvent(DirectConversationUiEvent.ReplyToMessage(messageId))
                }
            },
            onReactionClick = { emoji ->
                contextState.message?.id?.let { messageId ->
                    onUiEvent(DirectConversationUiEvent.MessageReactionSelected(messageId, emoji))
                }
            },
            showEdit = contextState.canEdit,
            onEditClick = {
                contextState.message?.id?.let { messageId ->
                    onUiEvent(DirectConversationUiEvent.EditMessage(messageId))
                }
            },
            onCopyClick = {
                contextState.message?.textPart?.text?.takeIf(String::isNotBlank)?.let(clipboardWriter::copyText)
                activeContextAnchor?.let { contextAnchor ->
                    feedbackOverlay =
                        FeedbackOverlayData(
                            anchor = contextAnchor.overlayAnchor,
                            text = copiedText,
                            color = contextMenuColor
                        )
                }
            },
            onDeleteClick = {
                contextState.message?.id?.let { messageId ->
                    onUiEvent(DirectConversationUiEvent.DeleteMessage(messageId))
                }
            },
            modifier = Modifier.fillMaxSize(),
            preview = {
                contextState.message?.let { message ->
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
                        errorMessage = errorMessage,
                        onUiEvent = onUiEvent,
                        onManualIdentitySetup = { showIdentitySetupDialog = true }
                    )
                },
                bottomBar = { containerColor ->
                    BottomBar(
                        composerState = composerState,
                        typingState = typingState,
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
                    onContextMessageRequested = { anchor ->
                        messageContextAnchor = anchor
                        onUiEvent(DirectConversationUiEvent.MessageContextRequested(anchor.messageId))
                    },
                    onReactionBurstRequested = { reactionBurst = it },
                    onRetryMessage = { messageId ->
                        onUiEvent(DirectConversationUiEvent.RetryMessage(messageId))
                    },
                    onSafetyWarningClick = { messageId, warning ->
                        onUiEvent(
                            DirectConversationUiEvent.SafetyWarningClicked(
                                messageId = messageId,
                                warning = warning
                            )
                        )
                    },
                    onAttachmentVisible = { attachmentId ->
                        onUiEvent(DirectConversationUiEvent.AttachmentVisible(attachmentId))
                    },
                    onAttachmentClick = { messageId, attachmentId ->
                        viewerMessageId = messageId
                        viewerAttachmentId = attachmentId
                        onUiEvent(DirectConversationUiEvent.AttachmentVisible(attachmentId))
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
                MessageReactionBurstOverlay(
                    burst = burst,
                    onDismiss = { reactionBurst = null }
                )
            }
        }
    }

    if (showIdentitySetupDialog) {
        IdentitySetupDialog(
            onShareIdentity = {
                showIdentitySetupDialog = false
                onUiEvent(DirectConversationUiEvent.ShareIdentityClicked)
            },
            onImportIdentity = {
                showIdentitySetupDialog = false
                onUiEvent(DirectConversationUiEvent.ImportIdentityClicked)
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
                    onUiEvent(DirectConversationUiEvent.ShareContact(sharedContact))
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
                onUiEvent(DirectConversationUiEvent.AddSharedContact(contact))
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
                onUiEvent(DirectConversationUiEvent.AttachmentVisible(attachmentId))
            },
            onError = { error -> onUiEvent(DirectConversationUiEvent.AttachmentError(error)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    uiState: DirectConversationUiState,
    containerColor: Color,
    errorMessage: String?,
    onUiEvent: (DirectConversationUiEvent) -> Unit,
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
                    modifier = Modifier.clickable { onUiEvent(DirectConversationUiEvent.HeaderClicked) },
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
                IconButton(onClick = { onUiEvent(DirectConversationUiEvent.BackClicked) }) {
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
                onVerifyIdentity = { onUiEvent(DirectConversationUiEvent.VerifyIdentityClicked) },
                onManualIdentitySetup = onManualIdentitySetup
            )
        }

        errorMessage?.let { message -> ErrorMessage(message = message) }
    }
}

@Composable
private fun BottomBar(
    composerState: MessageComposerUiState,
    typingState: TypingUiState,
    containerColor: Color,
    onUiEvent: (DirectConversationUiEvent) -> Unit,
    onContactAttachmentClick: () -> Unit
) {
    ChatComposerBar(
        composerState = composerState,
        typingState = typingState,
        containerColor = containerColor,
        onMessageTextChanged = { onUiEvent(DirectConversationUiEvent.MessageTextChanged(it)) },
        onSendClick = { onUiEvent(DirectConversationUiEvent.SendClicked) },
        onCancelReply = { onUiEvent(DirectConversationUiEvent.CancelReply) },
        onCancelEdit = { onUiEvent(DirectConversationUiEvent.CancelEdit) },
        onMediaSelected = { onUiEvent(DirectConversationUiEvent.MediaSelected(it)) },
        onOpenFilePicker = { onUiEvent(DirectConversationUiEvent.OpenFilePicker(it)) },
        onContactAttachmentClick = onContactAttachmentClick,
        onLocationCaptureStarted = { onUiEvent(DirectConversationUiEvent.LocationCaptureStarted) },
        onLocationCaptured = { onUiEvent(DirectConversationUiEvent.ShareCurrentLocation(it)) },
        onLocationCaptureFailed = { onUiEvent(DirectConversationUiEvent.LocationCaptureFailed(it)) },
        onAttachmentError = { onUiEvent(DirectConversationUiEvent.AttachmentError(it)) }
    )
}

@Composable
private fun Content(
    uiState: DirectConversationUiState,
    listState: LazyListState,
    innerPadding: PaddingValues,
    targetMessageId: String?,
    selectedContextMessageId: String?,
    onContextMessageRequested: (MessageContextAnchor) -> Unit,
    onReactionBurstRequested: (MessageReactionBurst) -> Unit,
    onRetryMessage: (String) -> Unit,
    onSafetyWarningClick: (String, MessageSafetyWarningUi) -> Unit,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String, String) -> Unit,
    onContactClick: (SharedContact) -> Unit
) {
    val fillModifier = Modifier.fillMaxSize().padding(innerPadding)
    val dissolvingListState =
        rememberDissolvingMessageListState(
            messages = uiState.messages,
            idOf = { it.id },
            shouldDissolve = { !it.isMine }
        )

    when {
        uiState.isLoading -> LoadingContent(modifier = fillModifier)

        dissolvingListState.messages.isEmpty() -> EmptyContent(
            contactName = uiState.contactName,
            securityState = uiState.contactSecurityState,
            modifier = fillModifier
        )

        else -> MessageList(
            dissolvingListState = dissolvingListState,
            listState = listState,
            targetMessageId = targetMessageId,
            selectedContextMessageId = selectedContextMessageId,
            onContextMessageRequested = onContextMessageRequested,
            onReactionBurstRequested = onReactionBurstRequested,
            onRetryMessage = onRetryMessage,
            onSafetyWarningClick = { messageId, _, warning -> onSafetyWarningClick(messageId, warning) },
            onAttachmentVisible = onAttachmentVisible,
            onAttachmentClick = onAttachmentClick,
            onContactClick = onContactClick,
            contentPadding = innerPadding
        )
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
