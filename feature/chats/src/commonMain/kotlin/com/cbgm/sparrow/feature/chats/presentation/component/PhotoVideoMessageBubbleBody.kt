package com.cbgm.sparrow.feature.chats.presentation.component

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
import com.cbgm.sparrow.feature.chats.presentation.component.mapper.toMediaItem
import com.cbgm.sparrow.feature.chats.presentation.component.model.ImageVideoTypeUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.feature.media.presentation.component.MediaThumbnail

@Composable
internal fun PhotoVideoMessageBubbleBody(
    imageVideoParts: List<MessagePartUi.ImageVideoUi>,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String) -> Unit
) {
    Content(
        imageVideoParts = imageVideoParts,
        onAttachmentVisible = onAttachmentVisible,
        onAttachmentClick = onAttachmentClick
    )
}

@Composable
private fun Content(
    imageVideoParts: List<MessagePartUi.ImageVideoUi>,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String) -> Unit
) {
    val visiblePhotoVideoParts = imageVideoParts.take(MAX_PREVIEW_ATTACHMENTS)
    val hiddenCount = (imageVideoParts.size - MAX_PREVIEW_ATTACHMENTS).coerceAtLeast(0)

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
        ) {
            visiblePhotoVideoParts.take(ATTACHMENTS_PER_ROW).forEach { visiblePart ->
                MessageMediaPreview(
                    imageVideoPart = visiblePart,
                    onAttachmentVisible = onAttachmentVisible,
                    onAttachmentClick = onAttachmentClick
                )
            }
        }

        if (visiblePhotoVideoParts.size > ATTACHMENTS_PER_ROW || hiddenCount > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
            ) {
                visiblePhotoVideoParts.drop(ATTACHMENTS_PER_ROW).forEach { photoVideoPart ->
                    MessageMediaPreview(
                        imageVideoPart = photoVideoPart,
                        onAttachmentVisible = onAttachmentVisible,
                        onAttachmentClick = onAttachmentClick
                    )
                }

                if (hiddenCount > 0) {
                    MoreAttachment(
                        additionalCount = hiddenCount,
                        onClick = { onAttachmentClick(imageVideoParts[MAX_PREVIEW_ATTACHMENTS].id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageMediaPreview(
    imageVideoPart: MessagePartUi.ImageVideoUi,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String) -> Unit
) {
    LaunchedEffect(imageVideoPart.id, imageVideoPart.bytes) {
        if (imageVideoPart.bytes == null) onAttachmentVisible(imageVideoPart.id)
    }

    Surface(
        modifier =
            Modifier
                .size(Dimens.MessageAttachment.previewSize)
                .clickable { onAttachmentClick(imageVideoPart.id) },
        shape = MaterialTheme.shapes.extraSmall,
        color = FunctionalColors.MediaBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MediaThumbnail(
                media = imageVideoPart.toMediaItem(),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (imageVideoPart.bytes == null) {
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

            if (imageVideoPart.type == ImageVideoTypeUi.VIDEO) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .size(Dimens.MessageAttachment.previewPlayIconSize)
                )
            }
        }
    }
}

@Composable
private fun MoreAttachment(
    additionalCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier =
            Modifier
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

@Preview
@Composable
private fun MediaMessageBubbleBodyPreview() {
    SparrowTheme {
        PhotoVideoMessageBubbleBody(
            imageVideoParts =
                listOf(
                    MessagePartUi.ImageVideoUi(
                        id = "preview-image",
                        type = ImageVideoTypeUi.IMAGE,
                        mimeType = "image/jpeg",
                        byteSize = 0
                    ),
                    MessagePartUi.ImageVideoUi(
                        id = "preview-video",
                        type = ImageVideoTypeUi.VIDEO,
                        mimeType = "video/mp4",
                        byteSize = 0
                    ),
                    MessagePartUi.ImageVideoUi(
                        id = "preview-image-2",
                        type = ImageVideoTypeUi.IMAGE,
                        mimeType = "image/jpeg",
                        byteSize = 0
                    ),
                    MessagePartUi.ImageVideoUi(
                        id = "preview-image-3",
                        type = ImageVideoTypeUi.IMAGE,
                        mimeType = "image/jpeg",
                        byteSize = 0
                    )
                ),
            onAttachmentVisible = {},
            onAttachmentClick = {}
        )
    }
}

private const val MAX_PREVIEW_ATTACHMENTS = 3
private const val ATTACHMENTS_PER_ROW = 2
