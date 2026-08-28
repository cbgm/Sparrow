package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.cbgm.sparrow.feature.media.presentation.component.MediaSelectionPreview
import com.cbgm.sparrow.feature.media.presentation.component.previewMediaSelections
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_chat_typing
import org.jetbrains.compose.resources.stringResource

/**
 * Bündelt alle reinen UI-Zustandswerte für die Nachrichteneingabe.
 */
data class MessageInputState(
    val messageText: String = "",
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

/**
 * Bündelt alle Interaktionen und Klicks für die Nachrichteneingabe.
 * Jede Attachment-Aktion hat jetzt ihr eigenes, klares Lambda.
 */
data class MessageInputActions(
    val onValueChange: (String) -> Unit,
    val onSendClick: () -> Unit,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.base)
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
                        horizontal = MaterialTheme.spacing.base.times(6),
                        vertical = MaterialTheme.spacing.micro
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            MediaSelectionPreview(
                media = state.selectedMedia,
                onClick = actions.onSelectionClick,
                onRemove = actions.onMediaRemove,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.base)
            )
            MessageInput(
                value = state.messageText,
                onValueChange = actions.onValueChange,
                onSendClick = actions.onSendClick,
                inputEnabled = state.isInputEnabled,
                sendEnabled = state.isSendEnabled,
                hasAttachments = state.selectedMedia.isNotEmpty(),
                onAttachmentClick = { isAttachmentBarVisible = !isAttachmentBarVisible },
                isAttachmentVisible = isAttachmentBarVisible
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
                    isLocationInProgress = state.isLocationInProgress
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
