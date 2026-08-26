package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.presentation.component.AttachmentBar
import com.cbgm.sparrow.feature.media.presentation.component.FileSelectionPreview
import com.cbgm.sparrow.feature.media.presentation.component.MediaSelectionPreview
import com.cbgm.sparrow.feature.media.presentation.component.previewMediaSelections
import com.cbgm.sparrow.feature.media.presentation.model.FileSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_chat_typing
import org.jetbrains.compose.resources.stringResource

sealed interface AttachmentClick {
    data object OpenGallery : AttachmentClick

    data object OpenCamera : AttachmentClick

    data object OpenContacts : AttachmentClick

    data object OpenFile : AttachmentClick
}

@Composable
fun MessageControl(
    containerColor: Color,
    isTyping: Boolean,
    contactName: String,
    messageText: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isInputEnabled: Boolean,
    isSendEnabled: Boolean,
    selectedMedia: List<MediaSelection> = emptyList(),
    selectedFiles: List<FileSelection> = emptyList(),
    onMediaSelectionClick: () -> Unit = {},
    onMediaRemove: (String) -> Unit = {},
    onFileRemove: (String) -> Unit = {},
    isGalleryEnabled: Boolean = true,
    isCameraEnabled: Boolean = true,
    isFileEnabled: Boolean = true,
    onAttachmentButtonClick: (AttachmentClick) -> Unit
) {
    var isAttachmentBarVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.base)
        ) {
            Text(
                text =
                    if (isTyping) {
                        stringResource(
                            Res.string.feature_chats_chat_typing,
                            contactName
                        )
                    } else {
                        ""
                    },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.spacing.base.times(6),
                            vertical = MaterialTheme.spacing.micro
                        ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            MediaSelectionPreview(
                media = selectedMedia,
                onClick = onMediaSelectionClick,
                onRemove = onMediaRemove,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.base)
            )
            FileSelectionPreview(
                files = selectedFiles,
                onRemove = onFileRemove,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.base)
            )
            MessageInput(
                value = messageText,
                onValueChange = onValueChange,
                onSendClick = onSendClick,
                inputEnabled = isInputEnabled,
                sendEnabled = isSendEnabled,
                hasAttachments = selectedMedia.isNotEmpty() || selectedFiles.isNotEmpty(),
                onAttachmentClick = { isAttachmentBarVisible = !isAttachmentBarVisible },
                isAttachmentVisible = isAttachmentBarVisible
            )

            if (isAttachmentBarVisible) {
                AttachmentBar(
                    onClickCamera = {
                        isAttachmentBarVisible = false
                        onAttachmentButtonClick(AttachmentClick.OpenCamera)
                    },
                    onClickFile = {
                        isAttachmentBarVisible = false
                        onAttachmentButtonClick(AttachmentClick.OpenFile)
                    },
                    onClickGallery = {
                        isAttachmentBarVisible = false
                        onAttachmentButtonClick(AttachmentClick.OpenGallery)
                    },
                    onClickContact = {
                        isAttachmentBarVisible = false
                        onAttachmentButtonClick(AttachmentClick.OpenContacts)
                    },
                    isGalleryEnabled = isGalleryEnabled,
                    isCameraEnabled = isCameraEnabled,
                    isFileEnabled = isFileEnabled
                )
            }
        }
    }
}

@Preview
@Composable
private fun MessageControlPreview() {
    SparrowTheme {
        MessageControl(
            containerColor = MaterialTheme.colorScheme.background,
            isTyping = false,
            contactName = "Chris",
            messageText = "Here are the files",
            onValueChange = {},
            onSendClick = {},
            isInputEnabled = true,
            isSendEnabled = true,
            selectedMedia = previewMediaSelections(),
            selectedFiles =
                listOf(
                    FileSelection(
                        id = "preview-file",
                        bytes = ByteArray(1024),
                        mimeType = "application/pdf",
                        fileName = "document.pdf"
                    )
                ),
            onMediaSelectionClick = {},
            onMediaRemove = {},
            onFileRemove = {},
            isGalleryEnabled = true,
            isCameraEnabled = true,
            isFileEnabled = true,
            onAttachmentButtonClick = {}
        )
    }
}
