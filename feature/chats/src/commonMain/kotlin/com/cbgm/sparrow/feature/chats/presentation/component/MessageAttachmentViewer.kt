package com.cbgm.sparrow.feature.chats.presentation.component

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
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageSecurity
import com.cbgm.sparrow.feature.chats.domain.model.attachment.MessageMediaType
import com.cbgm.sparrow.feature.chats.presentation.attachment.mapper.toMediaItem
import com.cbgm.sparrow.feature.chats.presentation.attachment.platform.MessageMediaExportItem
import com.cbgm.sparrow.feature.chats.presentation.attachment.platform.rememberMessageMediaExporter
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleModel
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageMediaAttachmentModel
import com.cbgm.sparrow.feature.media.presentation.component.MediaViewer
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_media
import com.cbgm.sparrow.resources.feature_chats_save_to_camera_roll
import org.jetbrains.compose.resources.stringResource

/**
 * Chat-specific adapter around the reusable media viewer.
 *
 * The media feature owns paging, image loading, video thumbnails and playback. Chats only owns
 * message-specific behavior such as receiver-only camera-roll export.
 */
@Composable
internal fun MessageAttachmentViewer(
    message: MessageBubbleModel,
    selectedAttachmentId: String,
    onDismiss: () -> Unit,
    onEnsureAttachmentLoaded: (String) -> Unit,
    onError: (String) -> Unit
) {
    val attachments = message.mediaAttachments
    val selectedIndex = attachments.indexOfFirst { it.id == selectedAttachmentId }
    if (selectedIndex < 0) return

    val exporter = rememberMessageMediaExporter()
    val mediaLabel = stringResource(Res.string.feature_chats_media)
    val saveContentDescription = stringResource(Res.string.feature_chats_save_to_camera_roll)
    val canSave = !message.isMine
    var savePending by remember(message.id) { mutableStateOf(false) }

    val loadState = attachments.map { attachment -> attachment.id to (attachment.bytes != null) }
    LaunchedEffect(canSave, savePending, loadState) {
        if (!canSave || !savePending) return@LaunchedEffect

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
        media = attachments.map(MessageMediaAttachmentModel::toMediaItem),
        initialIndex = selectedIndex,
        onDismiss = onDismiss,
        onEnsureMediaLoaded = onEnsureAttachmentLoaded,
        title = { currentIndex, total -> "$mediaLabel ${currentIndex + 1}/$total" },
        topBarActions = {
            if (canSave) {
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
            message =
                MessageBubbleModel(
                    id = "preview-message",
                    text = "",
                    isMine = false,
                    security = MessageSecurity.END_TO_END_ENCRYPTED,
                    contentStatus = MessageContentStatus.READABLE,
                    deliveryStatus = MessageDeliveryStatus.DELIVERED,
                    mediaAttachments =
                        listOf(
                            MessageMediaAttachmentModel(
                                id = "preview-image",
                                type = MessageMediaType.IMAGE,
                                mimeType = "image/jpeg"
                            )
                        )
                ),
            selectedAttachmentId = "preview-image",
            onDismiss = {},
            onEnsureAttachmentLoaded = {},
            onError = {}
        )
    }
}
