package com.cbgm.sparrow.feature.attachments.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.attachmentColors
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentContent
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMediaItem
import com.cbgm.sparrow.feature.attachments.presentation.model.AttachmentUiState
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.media.device.rememberFileOpener
import com.cbgm.sparrow.feature.media.presentation.component.MediaThumbnail
import com.cbgm.sparrow.feature.media.util.toReadableByteSize
import kotlin.math.roundToLong

@Composable
fun MessageAttachments(
    attachments: List<MessageAttachmentUi>,
    onAttachmentClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenError: (String) -> Unit = {}
) {
    if (attachments.isEmpty()) return

    val previewAttachments =
        attachments.filter { attachment ->
            attachment is MessageAttachmentUi.ImageVideoAttachmentUi ||
                attachment is MessageAttachmentUi.LocationAttachmentUi
        }
    val fileItems = attachments.filterIsInstance<MessageAttachmentUi.FileAttachmentUi>()

    if (previewAttachments.isNotEmpty()) {
        MessageAttachmentGrid(
            attachments = previewAttachments,
            onAttachmentClick = onAttachmentClick,
            modifier = modifier
        )
    }

    if (fileItems.isNotEmpty()) {
        MessageFileList(
            attachments = fileItems,
            onOpenError = onOpenError,
            modifier = modifier
        )
    }
}

@Composable
private fun MessageAttachmentGrid(
    attachments: List<MessageAttachmentUi>,
    onAttachmentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleAttachments = attachments.take(MAX_PREVIEW_ATTACHMENTS)
    val hiddenCount = (attachments.size - MAX_PREVIEW_ATTACHMENTS).coerceAtLeast(0)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)) {
            visibleAttachments.take(ATTACHMENTS_PER_ROW).forEach { attachment ->
                MessageAttachmentPreview(
                    attachment = attachment,
                    onAttachmentClick = onAttachmentClick
                )
            }
        }

        if (visibleAttachments.size > ATTACHMENTS_PER_ROW || hiddenCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)) {
                visibleAttachments.drop(ATTACHMENTS_PER_ROW).forEach { attachment ->
                    MessageAttachmentPreview(
                        attachment = attachment,
                        onAttachmentClick = onAttachmentClick
                    )
                }

                if (hiddenCount > 0) {
                    MoreAttachment(
                        additionalCount = hiddenCount,
                        onClick = { onAttachmentClick(attachments[MAX_PREVIEW_ATTACHMENTS].id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageAttachmentPreview(
    attachment: MessageAttachmentUi,
    onAttachmentClick: (String) -> Unit
) {
    when (attachment) {
        is MessageAttachmentUi.ImageVideoAttachmentUi ->
            MessageVisualAttachment(
                attachment = attachment,
                onAttachmentClick = onAttachmentClick
            )

        is MessageAttachmentUi.LocationAttachmentUi ->
            MessageLocationAttachment(
                attachment = attachment,
                onAttachmentClick = onAttachmentClick
            )

        is MessageAttachmentUi.FileAttachmentUi,
        is MessageAttachmentUi.ContactAttachmentUi -> Unit
    }
}

@Composable
private fun MessageLocationAttachment(
    attachment: MessageAttachmentUi.LocationAttachmentUi,
    onAttachmentClick: (String) -> Unit
) {
    val state = rememberAttachmentUiState(attachment.target)
    val location =
        (state as? AttachmentUiState.Ready)
            ?.content
            ?.let { content -> content as? AttachmentContent.Location }
            ?.location

    Surface(
        modifier =
            Modifier
                .size(Dimens.MessageAttachment.previewSize)
                .clickable(enabled = location != null) { onAttachmentClick(attachment.id) },
        shape = MaterialTheme.shapes.medium,
        color = FunctionalColors.MediaBackground,
        border = BorderStroke(
            Dimens.Base.borderStrokeWidth,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (location == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.MessageAttachment.loadingIndicatorSize),
                    strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
                )
            } else {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.micro),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = MaterialTheme.attachmentColors.location
                    )
                    Text(
                        text = "${location.latitude.toCoordinateText()}\n${location.longitude.toCoordinateText()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageVisualAttachment(
    attachment: MessageAttachmentUi.ImageVideoAttachmentUi,
    onAttachmentClick: (String) -> Unit
) {
    val state = rememberAttachmentUiState(attachment.target)
    val localFilePath =
        (state as? AttachmentUiState.Ready)
            ?.content
            ?.let { content -> content as? AttachmentContent.LocalFile }
            ?.localFilePath

    Surface(
        modifier =
            Modifier
                .size(Dimens.MessageAttachment.previewSize)
                .clickable { onAttachmentClick(attachment.id) },
        shape = MaterialTheme.shapes.medium,
        color = FunctionalColors.MediaBackground,
        border = BorderStroke(
            Dimens.Base.borderStrokeWidth,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (localFilePath != null) {
                MediaThumbnail(
                    media = attachment.toMediaItem(localFilePath),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (state is AttachmentUiState.Idle || state is AttachmentUiState.Loading) {
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

            if (attachment.type == MessageAttachmentType.VIDEO) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = MaterialTheme.shapes.circle,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(MaterialTheme.spacing.micro)
                            .size(Dimens.MessageAttachment.previewPlayIconSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageFileList(
    attachments: List<MessageAttachmentUi.FileAttachmentUi>,
    onOpenError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val opener = rememberFileOpener()
    var pendingFileId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        attachments.forEach { attachment ->
            val isOpening = pendingFileId == attachment.id
            val state = rememberAttachmentUiState(attachment.target, load = isOpening)
            val localFilePath =
                (state as? AttachmentUiState.Ready)
                    ?.content
                    ?.let { content -> content as? AttachmentContent.LocalFile }
                    ?.localFilePath

            LaunchedEffect(isOpening, localFilePath) {
                if (!isOpening || localFilePath == null) return@LaunchedEffect

                opener.open(
                    localFilePath = localFilePath,
                    fileName = attachment.fileName,
                    mimeType = attachment.mimeType
                ).onFailure { error ->
                    SparrowLog.error("MessageAttachments", "File could not be opened", error)
                    onOpenError(error.message ?: "File could not be opened")
                }
                pendingFileId = null
            }

            LaunchedEffect(isOpening, state) {
                if (isOpening && state is AttachmentUiState.Error) {
                    pendingFileId = null
                }
            }

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isOpening) { pendingFileId = attachment.id },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    Dimens.Base.borderStrokeWidth,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(MaterialTheme.spacing.base),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOpening) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.MessageAttachment.filePreviewIconSize),
                            strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.MessageAttachment.filePreviewIconSize)
                        )
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.base))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = attachment.byteSize.toReadableByteSize(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            Dimens.Base.borderStrokeWidth,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(MaterialTheme.spacing.micro)
        ) {
            Text(
                text = "+$additionalCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun Double.toCoordinateText(): String =
    ((this * COORDINATE_PRECISION_FACTOR).roundToLong() / COORDINATE_PRECISION_FACTOR).toString()

@Preview
@Composable
private fun MessageAttachmentsPreview() {
    SparrowTheme {
        MessageAttachments(
            attachments =
                listOf(
                    MessageAttachmentUi.ImageVideoAttachmentUi(
                        id = "preview-image",
                        type = MessageAttachmentType.IMAGE,
                        mimeType = "image/jpeg",
                        byteSize = 0
                    )
                ),
            onAttachmentClick = {}
        )
    }
}

private const val MAX_PREVIEW_ATTACHMENTS = 3
private const val ATTACHMENTS_PER_ROW = 2
private const val COORDINATE_PRECISION_FACTOR = 100_000.0
