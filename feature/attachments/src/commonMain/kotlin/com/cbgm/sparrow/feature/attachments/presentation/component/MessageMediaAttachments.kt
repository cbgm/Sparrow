package com.cbgm.sparrow.feature.attachments.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.MessageMediaType
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMediaItem
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageMediaAttachmentUi
import com.cbgm.sparrow.feature.media.presentation.component.MediaThumbnail

@Composable
fun MessageMediaAttachments(
    attachments: List<MessageMediaAttachmentUi>,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    val previewAttachments = attachments.take(MAX_PREVIEW_ATTACHMENTS)
    val hiddenCount = (attachments.size - MAX_PREVIEW_ATTACHMENTS).coerceAtLeast(0)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)) {
            previewAttachments.take(ATTACHMENTS_PER_ROW).forEach { attachment ->
                MessageMediaAttachment(
                    attachment = attachment,
                    onAttachmentVisible = onAttachmentVisible,
                    onAttachmentClick = onAttachmentClick
                )
            }
        }

        if (previewAttachments.size > ATTACHMENTS_PER_ROW || hiddenCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)) {
                previewAttachments.drop(ATTACHMENTS_PER_ROW).forEach { attachment ->
                    MessageMediaAttachment(
                        attachment = attachment,
                        onAttachmentVisible = onAttachmentVisible,
                        onAttachmentClick = onAttachmentClick
                    )
                }

                if (hiddenCount > 0) {
                    MoreMediaAttachment(
                        additionalCount = hiddenCount,
                        onClick = { onAttachmentClick(attachments[MAX_PREVIEW_ATTACHMENTS].id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageMediaAttachment(
    attachment: MessageMediaAttachmentUi,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String) -> Unit
) {
    LaunchedEffect(attachment.id, attachment.bytes) {
        if (attachment.bytes == null) onAttachmentVisible(attachment.id)
    }

    Surface(
        modifier =
            Modifier
                .size(Dimens.MessageAttachment.previewSize)
                .clickable { onAttachmentClick(attachment.id) },
        shape = MaterialTheme.shapes.extraSmall,
        color = FunctionalColors.MediaBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MediaThumbnail(
                media = attachment.toMediaItem(),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (attachment.bytes == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.MessageAttachment.loadingIndicatorSize),
                        strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
                    )
                }
            }

            if (attachment.type == MessageMediaType.VIDEO) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(Dimens.MessageAttachment.previewPlayIconSize)
                )
            }
        }
    }
}

@Composable
private fun MoreMediaAttachment(
    additionalCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(Dimens.MessageAttachment.previewSize)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraSmall,
        color = FunctionalColors.MediaBackground
    ) {
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.padding(MaterialTheme.spacing.micro)
        ) {
            Text(
                text = "+$additionalCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private const val MAX_PREVIEW_ATTACHMENTS = 3
private const val ATTACHMENTS_PER_ROW = 2

@Preview
@Composable
private fun MessageMediaAttachmentsPreview() {
    SparrowTheme {
        MessageMediaAttachments(
            attachments =
                listOf(
                    MessageMediaAttachmentUi(
                        id = "preview-image-1",
                        type = MessageMediaType.IMAGE,
                        mimeType = "image/jpeg",
                        bytes = byteArrayOf()
                    ),
                    MessageMediaAttachmentUi(
                        id = "preview-image-2",
                        type = MessageMediaType.IMAGE,
                        mimeType = "image/png",
                        bytes = byteArrayOf()
                    ),
                    MessageMediaAttachmentUi(
                        id = "preview-video",
                        type = MessageMediaType.VIDEO,
                        mimeType = "video/mp4",
                        bytes = byteArrayOf()
                    ),
                    MessageMediaAttachmentUi(
                        id = "preview-image-3",
                        type = MessageMediaType.IMAGE,
                        mimeType = "image/jpeg",
                        bytes = byteArrayOf()
                    )
                ),
            onAttachmentVisible = {},
            onAttachmentClick = {}
        )
    }
}
