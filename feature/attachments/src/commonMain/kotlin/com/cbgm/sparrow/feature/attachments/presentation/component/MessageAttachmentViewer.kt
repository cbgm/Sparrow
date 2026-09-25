package com.cbgm.sparrow.feature.attachments.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.feature.attachments.device.rememberLocationOpener
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentContent
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMediaExportItem
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMediaItem
import com.cbgm.sparrow.feature.attachments.presentation.model.AttachmentUiState
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.media.device.rememberMediaExporter
import com.cbgm.sparrow.feature.media.presentation.component.MediaViewer
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_attachments_media
import org.jetbrains.compose.resources.stringResource

@Composable
fun MessageAttachmentViewer(
    attachments: List<MessageAttachmentUi>,
    selectedAttachmentId: String,
    canSaveToCameraRoll: Boolean,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
) {
    val selectedAttachment =
        attachments.firstOrNull { attachment ->
            attachment.id == selectedAttachmentId
        } ?: return

    when (selectedAttachment) {
        is MessageAttachmentUi.ImageVideoAttachmentUi ->
            MessageMediaViewer(
                attachments = attachments.filterIsInstance<MessageAttachmentUi.ImageVideoAttachmentUi>(),
                selectedAttachmentId = selectedAttachmentId,
                canSaveToCameraRoll = canSaveToCameraRoll,
                onDismiss = onDismiss,
                onError = onError
            )

        is MessageAttachmentUi.LocationAttachmentUi ->
            MessageLocationViewer(
                attachment = selectedAttachment,
                onDismiss = onDismiss,
                onError = onError
            )

        is MessageAttachmentUi.FileAttachmentUi,
        is MessageAttachmentUi.ContactAttachmentUi -> Unit
    }
}

@Composable
private fun MessageMediaViewer(
    attachments: List<MessageAttachmentUi.ImageVideoAttachmentUi>,
    selectedAttachmentId: String,
    canSaveToCameraRoll: Boolean,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
) {
    val selectedIndex =
        attachments.indexOfFirst { attachment ->
            attachment.id == selectedAttachmentId
        }

    if (selectedIndex < 0) return

    val exporter = rememberMediaExporter()
    val mediaLabel = stringResource(Res.string.feature_attachments_media)
    var savePending by remember(selectedAttachmentId) { mutableStateOf(false) }

    val loadedMedia =
        attachments.map { attachment ->
            val state = rememberAttachmentUiState(attachment.target)
            val localFilePath =
                (state as? AttachmentUiState.Ready)
                    ?.content
                    ?.let { content -> content as? AttachmentContent.LocalFile }
                    ?.localFilePath
            attachment to localFilePath
        }

    LaunchedEffect(canSaveToCameraRoll, savePending, loadedMedia) {
        if (!canSaveToCameraRoll || !savePending) return@LaunchedEffect
        if (loadedMedia.any { (_, localFilePath) -> localFilePath == null }) return@LaunchedEffect

        exporter
            .saveToCameraRoll(
                loadedMedia.map { (attachment, localFilePath) ->
                    attachment.toMediaExportItem(requireNotNull(localFilePath))
                }
            ).onFailure { error ->
                onError(error.message ?: "Could not save media to camera roll")
            }

        savePending = false
    }

    MediaViewer(
        media =
            loadedMedia.map { (attachment, localFilePath) ->
                attachment.toMediaItem(localFilePath)
            },
        initialIndex = selectedIndex,
        onDismiss = onDismiss,
        title = { currentIndex, total ->
            "$mediaLabel ${currentIndex + 1}/$total"
        },
        topBarActions = {
            if (canSaveToCameraRoll) {
                IconButton(
                    onClick = { savePending = true },
                    enabled = !savePending,
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                        shape = MaterialTheme.shapes.circle
                    )
                ) {
                    if (savePending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.MessageAttachment.loadingIndicatorSize),
                            strokeWidth = Dimens.Base.progressIndicatorStrokeWidth,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun MessageLocationViewer(
    attachment: MessageAttachmentUi.LocationAttachmentUi,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
) {
    val locationOpener = rememberLocationOpener()
    val state = rememberAttachmentUiState(attachment.target)
    val location =
        (state as? AttachmentUiState.Ready)
            ?.content
            ?.let { content -> content as? AttachmentContent.Location }
            ?.location

    LaunchedEffect(attachment.id, location) {
        val loadedLocation = location ?: return@LaunchedEffect

        locationOpener
            .open(loadedLocation)
            .onFailure { error ->
                onError(error.message ?: "Location could not be opened")
            }

        onDismiss()
    }
}

@Preview
@Composable
private fun MessageAttachmentViewerPreview() {
    SparrowTheme {
        MessageAttachmentViewer(
            attachments =
                listOf(
                    MessageAttachmentUi.ImageVideoAttachmentUi(
                        id = "preview-image",
                        type = MessageAttachmentType.IMAGE,
                        mimeType = "image/jpeg",
                        byteSize = 0
                    ),
                    MessageAttachmentUi.ImageVideoAttachmentUi(
                        id = "preview-video",
                        type = MessageAttachmentType.VIDEO,
                        mimeType = "video/mp4",
                        byteSize = 0
                    )
                ),
            selectedAttachmentId = "preview-image",
            canSaveToCameraRoll = true,
            onDismiss = {},
            onError = {}
        )
    }
}
