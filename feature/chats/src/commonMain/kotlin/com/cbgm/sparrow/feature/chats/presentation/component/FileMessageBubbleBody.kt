package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
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
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.feature.media.device.rememberFileOpener
import com.cbgm.sparrow.feature.media.util.toReadableByteSize

@Composable
internal fun FileMessageBubbleBody(
    fileParts: List<MessagePartUi.FileUi>,
    onAttachmentVisible: (String) -> Unit
) {
    if (fileParts.isEmpty()) return

    Content(
        fileParts = fileParts,
        onAttachmentVisible = onAttachmentVisible
    )
}

@Composable
private fun Content(
    fileParts: List<MessagePartUi.FileUi>,
    onAttachmentVisible: (String) -> Unit
) {
    val opener = rememberFileOpener()
    var pendingFileId by remember { mutableStateOf<String?>(null) }
    val pendingFile = pendingFileId?.let { id -> fileParts.firstOrNull { it.id == id } }

    LaunchedEffect(pendingFileId, pendingFile?.localFilePath) {
        val file = pendingFile ?: return@LaunchedEffect
        val localFilePath = file.localFilePath ?: return@LaunchedEffect

        opener.open(
            localFilePath = localFilePath,
            fileName = file.fileName,
            mimeType = file.mimeType
        )
        pendingFileId = null
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        fileParts.forEach { attachment ->
            val isOpening = pendingFileId == attachment.id

            Surface(
                modifier = Modifier.clickable(enabled = !isOpening) {
                    pendingFileId = attachment.id
                    if (attachment.localFilePath == null) {
                        onAttachmentVisible(attachment.id)
                    }
                },
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(MaterialTheme.spacing.micro),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOpening) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.MessageAttachment.filePreviewIconSize),
                            strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.MessageAttachment.filePreviewIconSize)
                        )
                    }

                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.base))

                    Column {
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

@Preview
@Composable
private fun FileMessageBubbleBodyPreview() {
    SparrowTheme {
        FileMessageBubbleBody(
            fileParts =
                listOf(
                    MessagePartUi.FileUi(
                        id = "preview-file",
                        mimeType = "application/pdf",
                        byteSize = 1_048_576,
                        fileName = "document.pdf",
                        localFilePath = ""
                    ),
                    MessagePartUi.FileUi(
                        id = "preview-file-2",
                        mimeType = "text/plain",
                        byteSize = 42_000,
                        fileName = "notes.txt",
                        localFilePath = ""
                    )
                ),
            onAttachmentVisible = {}
        )
    }
}
