package com.cbgm.sparrow.feature.chats.presentation.direct

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.cbgm.sparrow.feature.chats.presentation.common.composer.ComposerContent
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.IndicatorUiState
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.MessageComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.common.header.HeaderContent
import com.cbgm.sparrow.feature.chats.presentation.common.header.component.SecurityBanner
import com.cbgm.sparrow.feature.chats.presentation.common.header.component.securityDescription
import com.cbgm.sparrow.feature.chats.presentation.common.header.mapper.toHeaderUiModel
import com.cbgm.sparrow.feature.chats.presentation.common.history.HistoryContent
import com.cbgm.sparrow.feature.chats.presentation.common.history.component.AddSharedContactDialog
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
import com.cbgm.sparrow.feature.chats.presentation.direct.component.IdentitySetupDialog
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiEvent
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.direct.model.findMessage
import com.cbgm.sparrow.feature.contacts.presentation.overview.ContactAttachmentSelectionRoute
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.common_copied
import com.cbgm.sparrow.resources.feature_chats_reconnect_cancel
import com.cbgm.sparrow.resources.feature_chats_reconnect_confirm
import com.cbgm.sparrow.resources.feature_chats_reconnect_contact
import com.cbgm.sparrow.resources.feature_chats_reconnect_warning
import com.cbgm.sparrow.resources.feature_chats_start_conversation_with
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectConversationScreen(
    uiState: DirectConversationUiState,
    composerState: MessageComposerUiState,
    contextState: MessageContextUiState<MessageBubbleUi>,
    indicatorState: IndicatorUiState,
    historyState: MessageHistoryUiState,
    modifier: Modifier = Modifier,
    onUiEvent: (DirectConversationUiEvent) -> Unit,
    onForwardMessageRequested: (String) -> Unit,
    onReconnectRequested: () -> Unit = {},
    reconnectBusy: Boolean = false,
    targetMessageId: String? = null
) {
    var showIdentitySetupDialog by rememberSaveable { mutableStateOf(false) }
    var showReconnectConfirmation by rememberSaveable { mutableStateOf(false) }
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

    if (showReconnectConfirmation) {
        AlertDialog(
            onDismissRequest = { showReconnectConfirmation = false },
            title = { Text(stringResource(Res.string.feature_chats_reconnect_contact)) },
            text = { Text(stringResource(Res.string.feature_chats_reconnect_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    showReconnectConfirmation = false
                    onReconnectRequested()
                }) { Text(stringResource(Res.string.feature_chats_reconnect_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showReconnectConfirmation = false }) {
                    Text(stringResource(Res.string.feature_chats_reconnect_cancel))
                }
            }
        )
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
            onForwardClick = {
                contextState.message?.id?.let(onForwardMessageRequested)
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
                contextState.message?.textPart?.text?.takeIf(String::isNotBlank)
                    ?.let(clipboardWriter::copyText)
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
                    HeaderContent(
                        model = uiState.toHeaderUiModel(),
                        containerColor = containerColor,
                        onBackClick = { onUiEvent(DirectConversationUiEvent.BackClicked) },
                        onHeaderClick = { onUiEvent(DirectConversationUiEvent.HeaderClicked) },
                        actions = {
                            IconButton(
                                onClick = { showReconnectConfirmation = true },
                                enabled = !reconnectBusy && !uiState.isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(Res.string.feature_chats_reconnect_contact)
                                )
                            }
                        }
                    ) {
                        if (!uiState.isLoading) {
                            SecurityBanner(
                                securityState = uiState.contactSecurityState,
                                identitySetupMode = uiState.identitySetupMode,
                                isChatAuthorized = uiState.isChatAuthorized,
                                onVerifyIdentity = { onUiEvent(DirectConversationUiEvent.VerifyIdentityClicked) },
                                onManualIdentitySetup = { showIdentitySetupDialog = true }
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
                        emptyTitle = stringResource(Res.string.feature_chats_start_conversation_with, uiState.contactName),
                        emptyDescription = securityDescription(uiState.contactSecurityState)
                    ),
                    listState = listState,
                    innerPadding = innerPadding,
                    targetMessageId = targetMessageId,
                    selectedContextMessageId = messageContextAnchor?.messageId,
                    historyState = historyState,
                    onLoadOlderMessages = { onUiEvent(DirectConversationUiEvent.LoadOlderMessages) },
                    onMessageHistoryTargetRequested = { messageId ->
                        onUiEvent(DirectConversationUiEvent.MessageHistoryTargetRequested(messageId))
                    },
                    onContextMessageRequested = { anchor ->
                        messageContextAnchor = anchor
                        onUiEvent(DirectConversationUiEvent.MessageContextRequested(anchor.messageId))
                    },
                    onReactionBurstRequested = { reactionBurst = it },
                    onRetryMessage = { messageId ->
                        onUiEvent(DirectConversationUiEvent.RetryMessage(messageId))
                    },
                    onSafetyWarningClick = { messageId, _, warning ->
                        onUiEvent(
                            DirectConversationUiEvent.SafetyWarningClicked(
                                messageId = messageId,
                                warning = warning
                            )
                        )
                    },
                    onAttachmentClick = { messageId, attachmentId ->
                        viewerMessageId = messageId
                        viewerAttachmentId = attachmentId
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

    IdentitySetupDialog(
        isVisible = showIdentitySetupDialog,
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

    val currentViewerMessage = uiState.findMessage(viewerMessageId)
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
            onError = { error -> onUiEvent(DirectConversationUiEvent.AttachmentError(error)) }
        )
    }
}

@Composable
private fun BottomBar(
    composerState: MessageComposerUiState,
    indicatorState: IndicatorUiState,
    containerColor: Color,
    onUiEvent: (DirectConversationUiEvent) -> Unit,
    onContactAttachmentClick: () -> Unit
) {
    ComposerContent(
        composerState = composerState,
        indicatorState = indicatorState,
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
        onAttachmentError = { onUiEvent(DirectConversationUiEvent.AttachmentError(it)) },
        onVoiceSendClick = { onUiEvent(DirectConversationUiEvent.VoiceSendClicked) }
    )
}
