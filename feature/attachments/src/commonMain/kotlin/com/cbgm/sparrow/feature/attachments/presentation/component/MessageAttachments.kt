package com.cbgm.sparrow.feature.attachments.presentation.component

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
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.attachmentColors
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMediaItem
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.media.device.rememberFileOpener
import com.cbgm.sparrow.feature.media.presentation.component.MediaThumbnail
import com.cbgm.sparrow.feature.media.util.toReadableByteSize
import kotlin.math.roundToLong

@Composable
fun MessageAttachments(
    attachments: List<MessageAttachmentUi>,
    onAttachmentVisible: (String) -> Unit,
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
            onAttachmentVisible = onAttachmentVisible,
            onAttachmentClick = onAttachmentClick,
            modifier = modifier
        )
    }

    if (fileItems.isNotEmpty()) {
        MessageFileList(
            attachments = fileItems,
            onAttachmentVisible = onAttachmentVisible,
            onOpenError = onOpenError,
            modifier = modifier
        )
    }
}

@Composable
private fun MessageAttachmentGrid(
    attachments: List<MessageAttachmentUi>,
    onAttachmentVisible: (String) -> Unit,
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
                    onAttachmentVisible = onAttachmentVisible,
                    onAttachmentClick = onAttachmentClick
                )
            }
        }

        if (visibleAttachments.size > ATTACHMENTS_PER_ROW || hiddenCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)) {
                visibleAttachments.drop(ATTACHMENTS_PER_ROW).forEach { attachment ->
                    MessageAttachmentPreview(
                        attachment = attachment,
                        onAttachmentVisible = onAttachmentVisible,
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
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String) -> Unit
) {
    when (attachment) {
        is MessageAttachmentUi.ImageVideoAttachmentUi ->
            MessageVisualAttachment(
                attachment = attachment,
                onAttachmentVisible = onAttachmentVisible,
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
    Surface(
        modifier =
            Modifier
                .size(Dimens.MessageAttachment.previewSize)
                .clickable { onAttachmentClick(attachment.id) },
        shape = MaterialTheme.shapes.extraSmall,
        color = FunctionalColors.MediaBackground
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
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
                    text = "${attachment.location.latitude.toCoordinateText()}\n${attachment.location.longitude.toCoordinateText()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun MessageVisualAttachment(
    attachment: MessageAttachmentUi.ImageVideoAttachmentUi,
    onAttachmentVisible: (String) -> Unit,
    onAttachmentClick: (String) -> Unit
) {
    val isLoaded = attachment.localFilePath != null || attachment.bytes != null

    LaunchedEffect(attachment.id, attachment.localFilePath, attachment.bytes) {
        if (!isLoaded) onAttachmentVisible(attachment.id)
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

            if (!isLoaded) {
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
private fun MessageFileList(
    attachments: List<MessageAttachmentUi.FileAttachmentUi>,
    onAttachmentVisible: (String) -> Unit,
    onOpenError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val opener = rememberFileOpener()
    var pendingFileId by remember { mutableStateOf<String?>(null) }
    val pendingFile = pendingFileId?.let { id -> attachments.firstOrNull { it.id == id } }

    LaunchedEffect(pendingFileId, pendingFile?.localFilePath) {
        val file = pendingFile ?: return@LaunchedEffect
        val localFilePath = file.localFilePath ?: return@LaunchedEffect

        opener.open(
            localFilePath = localFilePath,
            fileName = file.fileName,
            mimeType = file.mimeType
        ).onFailure { error ->
            onOpenError(error.message ?: "File could not be opened")
        }
        pendingFileId = null
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        attachments.forEach { attachment ->
            val isOpening = pendingFileId == attachment.id
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isOpening) {
                            pendingFileId = attachment.id
                            if (attachment.localFilePath == null) {
                                onAttachmentVisible(attachment.id)
                            }
                        },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
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
            onAttachmentVisible = {},
            onAttachmentClick = {}
        )
    }
}

private fun Double.toCoordinateText(): String =
    ((this * LOCATION_COORDINATE_SCALE).roundToLong().toDouble() / LOCATION_COORDINATE_SCALE).toString()

private const val LOCATION_COORDINATE_SCALE = 100_000.0
private const val MAX_PREVIEW_ATTACHMENTS = 3
private const val ATTACHMENTS_PER_ROW = 2
