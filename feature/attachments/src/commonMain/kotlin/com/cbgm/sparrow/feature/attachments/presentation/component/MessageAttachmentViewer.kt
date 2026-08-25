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
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.feature.attachments.device.MessageMediaExportItem
import com.cbgm.sparrow.feature.attachments.device.rememberMessageMediaExporter
import com.cbgm.sparrow.feature.attachments.domain.model.MessageMediaType
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMediaItem
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageMediaAttachmentUi
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
    val selectedIndex = attachments.indexOfFirst { it.id == selectedAttachmentId }
    if (selectedIndex < 0) return

    val exporter = rememberMessageMediaExporter()
    val mediaLabel = stringResource(Res.string.feature_attachments_media)
    val saveContentDescription = stringResource(Res.string.feature_attachments_save_to_camera_roll)
    var savePending by remember(selectedAttachmentId) { mutableStateOf(false) }

    val loadState = attachments.map { attachment -> attachment.id to (attachment.bytes != null) }
    LaunchedEffect(canSaveToCameraRoll, savePending, loadState) {
        if (!canSaveToCameraRoll || !savePending) return@LaunchedEffect

        val missing = attachments.filter { attachment -> attachment.bytes == null }
        if (missing.isNotEmpty()) {
            missing.forEach { attachment -> onEnsureAttachmentLoaded(attachment.id) }
            return@LaunchedEffect
        }

        exporter.saveToCameraRoll(
            attachments.map { attachment ->
                MessageMediaExportItem(
                    attachmentId = attachment.id,
                    type = attachment.type,
                    mimeType = attachment.mimeType,
                    bytes = requireNotNull(attachment.bytes)
                )
            }
        ).onFailure { error ->
            onError(error.message ?: "Could not save media to camera roll")
        }
        savePending = false
    }

    MediaViewer(
        media = attachments.map(MessageMediaAttachmentUi::toMediaItem),
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
                        type = MessageMediaType.IMAGE,
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
