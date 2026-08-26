package com.cbgm.sparrow.feature.attachments.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageFileAttachmentUi
import com.cbgm.sparrow.feature.media.device.rememberFileOpener

@Composable
fun MessageFileAttachments(
    files: List<MessageFileAttachmentUi>,
    modifier: Modifier = Modifier,
    onAttachmentVisible: (String) -> Unit = {},
    onOpenError: (String) -> Unit = {}
) {
    if (files.isEmpty()) return

    val opener = rememberFileOpener()
    var pendingFileId by remember { mutableStateOf<String?>(null) }
    val pendingFile = pendingFileId?.let { id -> files.firstOrNull { it.id == id } }

    LaunchedEffect(pendingFileId, pendingFile?.bytes) {
        val file = pendingFile ?: return@LaunchedEffect
        val bytes = file.bytes ?: return@LaunchedEffect

        opener.open(
            fileName = file.fileName,
            mimeType = file.mimeType,
            bytes = bytes
        ).onFailure { error ->
            onOpenError(error.message ?: "File could not be opened")
        }
        pendingFileId = null
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        files.forEach { file ->
            val isOpening = pendingFileId == file.id && file.bytes != null
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isOpening) {
                            pendingFileId = file.id
                            if (file.bytes == null) onAttachmentVisible(file.id)
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
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.MessageAttachment.filePreviewIconSize)
                        )
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.base))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = file.sizeText,
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
private fun MessageFileAttachmentsPreview() {
    SparrowTheme {
        MessageFileAttachments(
            files =
                listOf(
                    MessageFileAttachmentUi(
                        id = "preview-file",
                        fileName = "project-plan.pdf",
                        mimeType = "application/pdf",
                        sizeText = "248 KB",
                        bytes = ByteArray(8)
                    )
                )
        )
    }
}
