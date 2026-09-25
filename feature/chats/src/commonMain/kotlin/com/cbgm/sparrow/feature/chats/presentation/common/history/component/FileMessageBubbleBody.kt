package com.cbgm.sparrow.feature.chats.presentation.common.history.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentContent
import com.cbgm.sparrow.feature.attachments.presentation.component.rememberAttachmentUiState
import com.cbgm.sparrow.feature.attachments.presentation.model.AttachmentUiState
import com.cbgm.sparrow.feature.chats.presentation.common.history.mapper.toAttachmentTarget
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessagePartUi
import com.cbgm.sparrow.feature.media.device.FileOpener
import com.cbgm.sparrow.feature.media.device.rememberFileOpener
import com.cbgm.sparrow.feature.media.util.toReadableByteSize

@Composable
internal fun FileMessageBubbleBody(
    fileParts: List<MessagePartUi.File>
) {
    val opener = rememberFileOpener()
    var openingFileId by remember { mutableStateOf<String?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        fileParts.forEach { attachment ->
            MessageFileItem(
                attachment = attachment,
                opener = opener,
                isOpening = openingFileId == attachment.id,
                onClick = { openingFileId = attachment.id },
                onOpened = { openingFileId = null }
            )
        }
    }
}

@Composable
private fun MessageFileItem(
    attachment: MessagePartUi.File,
    opener: FileOpener,
    isOpening: Boolean,
    onClick: () -> Unit,
    onOpened: () -> Unit
) {
    val attachmentState =
        rememberAttachmentUiState(
            target = attachment.toAttachmentTarget(),
            load = isOpening
        )
    val localFilePath =
        (attachmentState as? AttachmentUiState.Ready)
            ?.content
            ?.let { content -> content as? AttachmentContent.LocalFile }
            ?.localFilePath

    LaunchedEffect(isOpening, localFilePath) {
        if (!isOpening || localFilePath == null) return@LaunchedEffect

        opener.open(
            localFilePath = localFilePath,
            fileName = attachment.fileName,
            mimeType = attachment.mimeType
        )
        onOpened()
    }

    LaunchedEffect(isOpening, attachmentState) {
        if (isOpening && attachmentState is AttachmentUiState.Error) {
            onOpened()
        }
    }

    Surface(
        modifier = Modifier.clickable(enabled = !isOpening) { onClick() },
        shape = MaterialTheme.shapes.small,
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
                    modifier = Modifier.size(Dimens.MessageAttachment.filePreviewIconSize),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.base))

            Column {
                Text(
                    text = attachment.fileName,
                    style = MaterialTheme.typography.bodyMedium,
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

@Preview
@Composable
private fun FileMessageBubbleBodyPreview() {
    SparrowTheme {
        FileMessageBubbleBody(
            fileParts =
                listOf(
                    MessagePartUi.File(
                        id = "preview-file",
                        mimeType = "application/pdf",
                        byteSize = 1_048_576,
                        fileName = "document.pdf"
                    ),
                    MessagePartUi.File(
                        id = "preview-file-2",
                        mimeType = "text/plain",
                        byteSize = 42_000,
                        fileName = "notes.txt"
                    )
                )
        )
    }
}
