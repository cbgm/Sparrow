package com.cbgm.sparrow.feature.chats.presentation.common.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cbgm.sparrow.feature.attachments.device.rememberCurrentLocationLauncher
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.chats.presentation.common.composer.component.MessageControl
import com.cbgm.sparrow.feature.chats.presentation.common.composer.mapper.toComposerPreviewUi
import com.cbgm.sparrow.feature.chats.presentation.common.composer.mapper.toMessageInputState
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.ComposerPreviewUi
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.IndicatorUiState
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.MessageComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.MessageInputActions
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResult
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.feature.media.presentation.selection.rememberMediaSelectionLauncher

@Composable
fun ComposerContent(
    composerState: MessageComposerUiState,
    indicatorState: IndicatorUiState,
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
    onAttachmentError: (String) -> Unit,
    onVoiceSendClick: () -> Unit
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

    val composerPreview = composerState.toComposerPreviewUi()

    MessageControl(
        containerColor = containerColor,
        state = composerState.toMessageInputState(indicatorState, composerPreview),
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
                },
                onVoiceSendClick = onVoiceSendClick
            )
    )
}
