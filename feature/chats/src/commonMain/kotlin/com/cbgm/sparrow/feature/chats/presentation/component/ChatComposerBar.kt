package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cbgm.sparrow.feature.attachments.device.rememberCurrentLocationLauncher
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.chats.presentation.component.model.ComposerPreviewUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.component.model.TypingUiState
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResult
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.feature.media.presentation.selection.rememberMediaSelectionLauncher
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_edit_message
import com.cbgm.sparrow.resources.feature_chats_reply_you
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatComposerBar(
    composerState: MessageComposerUiState,
    typingState: TypingUiState,
    containerColor: Color,
    onMessageTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onCancelReply: () -> Unit,
    onCancelEdit: () -> Unit,
    onMediaSelected: (List<MediaSelection>) -> Unit,
    onOpenFilePicker: (String) -> Unit,
    onContactAttachmentClick: () -> Unit,
    onLocationCaptureStarted: () -> Unit,
    onLocationCaptured: (CurrentLocation) -> Unit,
    onLocationCaptureFailed: (String) -> Unit,
    onAttachmentError: (String) -> Unit
) {
    val currentLocationLauncher =
        rememberCurrentLocationLauncher(
            onLocation = onLocationCaptured,
            onError = onLocationCaptureFailed
        )

    val mediaPicker =
        rememberMediaSelectionLauncher(
            maxItems = MessageAttachmentPolicy.MAX_ATTACHMENTS_PER_MESSAGE,
            maxImageDimension = MessageAttachmentPolicy.MAX_IMAGE_DIMENSION,
            maxImageBytes = MessageAttachmentPolicy.MAX_IMAGE_BYTES,
            maxVideoBytes = MessageAttachmentPolicy.MAX_VIDEO_BYTES,
            maxFileBytes = MessageAttachmentPolicy.MAX_FILE_BYTES,
            selectedMedia = composerState.selectedMedia,
            onResult = { result ->
                when (result) {
                    is MediaSelectionResult.Selected -> onMediaSelected(result.media)
                    is MediaSelectionResult.Error -> onAttachmentError(result.message)
                    MediaSelectionResult.Dismissed -> Unit
                }
            },
            onFilePickerSessionStarted = onOpenFilePicker
        )

    val composerPreview =
        when {
            composerState.editingMessageId != null ->
                ComposerPreviewUi(
                    type = ComposerPreviewUi.Type.EDIT,
                    icon = Icons.Default.Edit,
                    iconText = stringResource(Res.string.feature_chats_edit_message)
                )

            composerState.replyTo != null ->
                ComposerPreviewUi(
                    type = ComposerPreviewUi.Type.REPLY,
                    iconText =
                        composerState.replyTo.senderName.takeUnless { it.isNullOrBlank() }
                            ?: stringResource(Res.string.feature_chats_reply_you),
                    additionalText = " - ${composerState.replyTo.previewText.orEmpty()}",
                    icon = Icons.AutoMirrored.Filled.Reply
                )

            else -> null
        }

    MessageControl(
        containerColor = containerColor,
        state =
            MessageInputState(
                messageText = composerState.messageText,
                composerPreview = composerPreview,
                isTyping = typingState.isTyping,
                contactName = typingState.displayName,
                isInputEnabled = composerState.availability.isInputEnabled,
                isSendEnabled = composerState.availability.isSendEnabled,
                isLocationInProgress = composerState.locationShareState.isInProgress,
                selectedMedia = composerState.selectedMedia,
                isGalleryEnabled = composerState.availability.canAddAttachment,
                isCameraEnabled = composerState.availability.canAddAttachment,
                isFileEnabled = composerState.availability.canAddAttachment
            ),
        actions =
            MessageInputActions(
                onValueChange = onMessageTextChanged,
                onSendClick = onSendClick,
                onCancelPreview = {
                    when (composerPreview?.type) {
                        ComposerPreviewUi.Type.REPLY -> onCancelReply()
                        ComposerPreviewUi.Type.EDIT -> onCancelEdit()
                        null -> Unit
                    }
                },
                onSelectionClick = mediaPicker::launch,
                onMediaRemove = { mediaId ->
                    onMediaSelected(composerState.selectedMedia.filterNot { it.id == mediaId })
                },
                onClickGallery = { mediaPicker.launch(MediaSelectionSource.GALLERY) },
                onClickCamera = { mediaPicker.launch(MediaSelectionSource.CAMERA) },
                onClickFile = { mediaPicker.launch(MediaSelectionSource.FILE_PICKER) },
                onClickContact = onContactAttachmentClick,
                onClickLocation = {
                    onLocationCaptureStarted()
                    currentLocationLauncher.launch()
                }
            )
    )
}
