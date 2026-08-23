package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.domain.model.attachment.MessageMediaType
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageMediaAttachmentModel
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
internal fun MessageMediaAttachments(
    attachments: List<MessageMediaAttachmentModel>,
    onAttachmentVisible: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        attachments.chunked(2).forEach { rowAttachments ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
            ) {
                rowAttachments.forEach { attachment ->
                    MessageMediaAttachment(
                        attachment = attachment,
                        onAttachmentVisible = onAttachmentVisible,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowAttachments.size == 1 && attachments.size > 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MessageMediaAttachment(
    attachment: MessageMediaAttachmentModel,
    onAttachmentVisible: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachment.type == MessageMediaType.IMAGE) {
        LaunchedEffect(attachment.id, attachment.bytes) {
            if (attachment.bytes == null) onAttachmentVisible(attachment.id)
        }
    }

    val bitmap =
        remember(attachment.id, attachment.bytes, attachment.type) {
            if (attachment.type == MessageMediaType.IMAGE) {
                attachment.bytes?.let { bytes -> runCatching { bytes.decodeToImageBitmap() }.getOrNull() }
            } else {
                null
            }
        }
    val width = attachment.width
    val height = attachment.height
    val aspectRatio =
        if (width != null && height != null && width > 0 && height > 0) {
            (width.toFloat() / height.toFloat()).coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
        } else {
            DEFAULT_ASPECT_RATIO
        }

    Surface(
        modifier =
            modifier
                .aspectRatio(aspectRatio)
                .then(
                    if (attachment.type == MessageMediaType.VIDEO) {
                        Modifier.clickable { onAttachmentVisible(attachment.id) }
                    } else {
                        Modifier
                    }
                ),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        when {
            bitmap != null ->
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

            attachment.type == MessageMediaType.VIDEO ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.MessageAttachment.loadingIndicatorSize)
                    )
                }

            else ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.MessageAttachment.loadingIndicatorSize),
                        strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
                    )
                }
        }
    }
}

private const val DEFAULT_ASPECT_RATIO = 1f
private const val MIN_ASPECT_RATIO = 0.75f
private const val MAX_ASPECT_RATIO = 1.5f
