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
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMediaExportItem
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMediaItem
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageMediaAttachmentUi
import com.cbgm.sparrow.feature.attachments.util.LocationAttachmentPayload
import com.cbgm.sparrow.feature.media.device.rememberMediaExporter
import com.cbgm.sparrow.feature.media.presentation.component.MediaViewer
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_attachments_media
import com.cbgm.sparrow.resources.feature_attachments_save_to_camera_roll
import org.jetbrains.compose.resources.stringResource

@Composable
fun MessageAttachmentViewer(
    attachments: List<MessageMediaAttachmentUi>,
    selectedAttachmentId: String,
    canSaveToCameraRoll: Boolean,
    onDismiss: () -> Unit,
    onEnsureAttachmentLoaded: (String) -> Unit,
    onError: (String) -> Unit
) {
    val selectedAttachment = attachments.firstOrNull { it.id == selectedAttachmentId } ?: return
    if (selectedAttachment.type == MessageAttachmentType.LOCATION) {
        val locationOpener = rememberLocationOpener()
        LaunchedEffect(selectedAttachment.id, selectedAttachment.bytes) {
            val bytes = selectedAttachment.bytes
            if (bytes == null) {
                onEnsureAttachmentLoaded(selectedAttachment.id)
                return@LaunchedEffect
            }

            val location = LocationAttachmentPayload.decode(bytes)
            if (location == null) {
                onError("Location could not be opened")
            } else {
                locationOpener.open(location)
                    .onFailure { error ->
                        onError(error.message ?: "Location could not be opened")
                    }
            }
            onDismiss()
        }
        return
    }

    val mediaAttachments = attachments.filter { it.type != MessageAttachmentType.LOCATION }
    val selectedIndex = mediaAttachments.indexOfFirst { it.id == selectedAttachmentId }
    if (selectedIndex < 0) return

    val exporter = rememberMediaExporter()
    val mediaLabel = stringResource(Res.string.feature_attachments_media)
    val saveContentDescription = stringResource(Res.string.feature_attachments_save_to_camera_roll)
    var savePending by remember(selectedAttachmentId) { mutableStateOf(false) }

    val loadState = mediaAttachments.map { attachment -> attachment.id to (attachment.bytes != null) }
    LaunchedEffect(canSaveToCameraRoll, savePending, loadState) {
        if (!canSaveToCameraRoll || !savePending) return@LaunchedEffect

        val missing = mediaAttachments.filter { attachment -> attachment.bytes == null }
        if (missing.isNotEmpty()) {
            missing.forEach { attachment -> onEnsureAttachmentLoaded(attachment.id) }
            return@LaunchedEffect
        }

        exporter.saveToCameraRoll(
            mediaAttachments.map(MessageMediaAttachmentUi::toMediaExportItem)
        ).onFailure { error ->
            onError(error.message ?: "Could not save media to camera roll")
        }
        savePending = false
    }

    MediaViewer(
        media = mediaAttachments.map(MessageMediaAttachmentUi::toMediaItem),
        initialIndex = selectedIndex,
        onDismiss = onDismiss,
        onEnsureMediaLoaded = onEnsureAttachmentLoaded,
        title = { currentIndex, total -> "$mediaLabel ${currentIndex + 1}/$total" },
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

@Preview
@Composable
private fun MessageAttachmentViewerPreview() {
    SparrowTheme {
        MessageAttachmentViewer(
            attachments =
                listOf(
                    MessageMediaAttachmentUi(
                        id = "preview-image",
                        type = MessageAttachmentType.IMAGE,
                        mimeType = "image/jpeg"
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
