package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.cbgm.sparrow.feature.chats.presentation.component.model.ComposerPreviewUi
import com.cbgm.sparrow.feature.media.presentation.component.MediaSelectionPreview
import com.cbgm.sparrow.feature.media.presentation.component.previewMediaSelections
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_chat_typing
import org.jetbrains.compose.resources.stringResource

data class MessageInputState(
    val messageText: String = "",
    val composerPreview: ComposerPreviewUi? = null,
    val isTyping: Boolean = false,
    val contactName: String = "",
    val isInputEnabled: Boolean = true,
    val isSendEnabled: Boolean = false,
    val isLocationInProgress: Boolean = false,
    val selectedMedia: List<MediaSelection> = emptyList(),
    val isGalleryEnabled: Boolean = true,
    val isCameraEnabled: Boolean = true,
    val isFileEnabled: Boolean = true
)

data class MessageInputActions(
    val onValueChange: (String) -> Unit,
    val onSendClick: () -> Unit,
    val onCancelPreview: () -> Unit = {},
    val onSelectionClick: (MediaSelectionSource) -> Unit = {},
    val onMediaRemove: (String) -> Unit = {},
    val onClickCamera: () -> Unit = {},
    val onClickFile: () -> Unit = {},
    val onClickGallery: () -> Unit = {},
    val onClickContact: () -> Unit = {},
    val onClickLocation: () -> Unit = {}
)

@Composable
fun MessageControl(
    state: MessageInputState,
    actions: MessageInputActions,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    var isAttachmentBarVisible by remember { mutableStateOf(false) }
    var locationProgressWasVisible by remember { mutableStateOf(false) }

    val isEditing = state.composerPreview?.type == ComposerPreviewUi.Type.EDIT

    LaunchedEffect(isEditing) {
        if (isEditing) {
            isAttachmentBarVisible = false
        }
    }

    LaunchedEffect(state.isLocationInProgress) {
        if (state.isLocationInProgress) {
            locationProgressWasVisible = true
        } else if (locationProgressWasVisible) {
            isAttachmentBarVisible = false
            locationProgressWasVisible = false
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor
    ) {
        val basePaddingHorizontal = MaterialTheme.spacing.base

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (state.isTyping) {
                    stringResource(
                        Res.string.feature_chats_chat_typing,
                        state.contactName
                    )
                } else {
                    ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = basePaddingHorizontal + MaterialTheme.spacing.base.times(6),
                        vertical = MaterialTheme.spacing.micro
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            state.composerPreview?.let { preview ->
                ComposerPreview(
                    previewUi = preview,
                    onCancel = actions.onCancelPreview,
                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.base)
                )
            }
            MediaSelectionPreview(
                media = state.selectedMedia,
                onClick = actions.onSelectionClick,
                onRemove = actions.onMediaRemove,
                modifier = Modifier.padding(
                    start = basePaddingHorizontal,
                    end = basePaddingHorizontal,
                    bottom = MaterialTheme.spacing.base
                )
            )
            MessageInput(
                value = state.messageText,
                onValueChange = actions.onValueChange,
                onSendClick = actions.onSendClick,
                inputEnabled = state.isInputEnabled,
                sendEnabled = state.isSendEnabled,
                hasAttachments = state.selectedMedia.isNotEmpty(),
                attachmentsEnabled = !isEditing,
                isEditing = isEditing,
                onAttachmentClick = { isAttachmentBarVisible = !isAttachmentBarVisible },
                isAttachmentVisible = isAttachmentBarVisible,
                modifier = Modifier.padding(horizontal = basePaddingHorizontal)
            )

            if (isAttachmentBarVisible) {
                AttachmentBar(
                    onClickCamera = {
                        isAttachmentBarVisible = false
                        actions.onClickCamera()
                    },
                    onClickFile = {
                        isAttachmentBarVisible = false
                        actions.onClickFile()
                    },
                    onClickGallery = {
                        isAttachmentBarVisible = false
                        actions.onClickGallery()
                    },
                    onClickContact = {
                        isAttachmentBarVisible = false
                        actions.onClickContact()
                    },
                    onClickLocation = {
                        actions.onClickLocation()
                    },
                    isGalleryEnabled = state.isGalleryEnabled,
                    isCameraEnabled = state.isCameraEnabled,
                    isFileEnabled = state.isFileEnabled,
                    isLocationInProgress = state.isLocationInProgress,
                    modifier = Modifier.padding(horizontal = basePaddingHorizontal)
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
            state = MessageInputState(
                isTyping = false,
                contactName = "Chris",
                messageText = "Here are the files",
                composerPreview =
                    ComposerPreviewUi(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        iconText = "Reply",
                        additionalText = "The original message should only appear as a short excerpt in the composer",
                        type = ComposerPreviewUi.Type.REPLY
                    ),
                isInputEnabled = true,
                isSendEnabled = true,
                selectedMedia = previewMediaSelections(),
                isGalleryEnabled = true,
                isCameraEnabled = true,
                isFileEnabled = true,
                isLocationInProgress = false
            ),
            actions = MessageInputActions(
                onValueChange = {},
                onSendClick = {},
                onCancelPreview = {},
                onSelectionClick = {},
                onMediaRemove = {},
                onClickCamera = {},
                onClickFile = {},
                onClickGallery = {},
                onClickContact = {},
                onClickLocation = {}
            )
        )
    }
}
