package com.cbgm.sparrow.feature.attachments.presentation.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.cbgm.sparrow.feature.attachments.device.rememberLocationOpener
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMediaExportItem
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMediaItem
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.media.device.rememberMediaExporter
import com.cbgm.sparrow.feature.media.presentation.component.MediaViewer
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_attachments_media
import com.cbgm.sparrow.resources.feature_attachments_save_to_camera_roll
import org.jetbrains.compose.resources.stringResource

@Composable
fun MessageAttachmentViewer(
    attachments: List<MessageAttachmentUi>,
    selectedAttachmentId: String,
    canSaveToCameraRoll: Boolean,
    onDismiss: () -> Unit,
    onEnsureAttachmentLoaded: (String) -> Unit,
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
                onEnsureAttachmentLoaded = onEnsureAttachmentLoaded,
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
    onEnsureAttachmentLoaded: (String) -> Unit,
    onError: (String) -> Unit
) {
    val selectedIndex =
        attachments.indexOfFirst { attachment ->
            attachment.id == selectedAttachmentId
        }

    if (selectedIndex < 0) return

    val exporter = rememberMediaExporter()
    val mediaLabel = stringResource(Res.string.feature_attachments_media)
    val saveContentDescription = stringResource(Res.string.feature_attachments_save_to_camera_roll)

    var savePending by remember(selectedAttachmentId) { mutableStateOf(false) }

    val loadState =
        attachments.map { attachment ->
            attachment.id to (attachment.localFilePath != null || attachment.bytes != null)
        }

    LaunchedEffect(
        canSaveToCameraRoll,
        savePending,
        loadState
    ) {
        if (!canSaveToCameraRoll || !savePending) return@LaunchedEffect

        val unloadedAttachments =
            attachments.filter { attachment ->
                attachment.localFilePath == null && attachment.bytes == null
            }

        if (unloadedAttachments.isNotEmpty()) {
            unloadedAttachments.forEach { attachment ->
                onEnsureAttachmentLoaded(attachment.id)
            }
            return@LaunchedEffect
        }

        exporter
            .saveToCameraRoll(attachments.map { attachment -> attachment.toMediaExportItem() })
            .onFailure { error ->
                onError(error.message ?: "Could not save media to camera roll")
            }

        savePending = false
    }

    MediaViewer(
        media = attachments.map { attachment -> attachment.toMediaItem() },
        initialIndex = selectedIndex,
        onDismiss = onDismiss,
        onEnsureMediaLoaded = onEnsureAttachmentLoaded,
        title = { currentIndex, total ->
            "$mediaLabel ${currentIndex + 1}/$total"
        },
        topBarActions = {
            if (canSaveToCameraRoll) {
                IconButton(
                    onClick = { savePending = true },
                    enabled = !savePending
                ) {
                    if (savePending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.MessageAttachment.loadingIndicatorSize),
                            strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = saveContentDescription
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

    LaunchedEffect(attachment.id, attachment.location) {
        locationOpener
            .open(attachment.location)
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
                    ),
                    MessageAttachmentUi.LocationAttachmentUi(
                        id = "preview-location",
                        location = CurrentLocation(latitude = 50.2586, longitude = 10.9644)
                    )
                ),
            selectedAttachmentId = "preview-image",
            canSaveToCameraRoll = true,
            onDismiss = {},
            onEnsureAttachmentLoaded = {},
            onError = {}
        )
    }
}
