package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing

@Composable
internal fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onVoiceClick: () -> Unit,
    inputEnabled: Boolean,
    sendEnabled: Boolean,
    hasAttachments: Boolean,
    modifier: Modifier = Modifier,
    attachmentsEnabled: Boolean = true,
    isEditing: Boolean = false,
    isAttachmentVisible: Boolean,
    onAttachmentClick: () -> Unit
) {
    var textLineCount by remember { mutableIntStateOf(1) }

    val isMultiline = textLineCount > 1

    val buttonWidth = Dimens.MessageInput.sendButtonWidth
    val buttonHeight = Dimens.MessageInput.buttonHeight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars
            )
            .imePadding(),
        verticalAlignment = Alignment.Bottom
    ) {
        RoundedInputButton(
            onClick = onAttachmentClick,
            enabled = attachmentsEnabled,
            modifier = Modifier
                .padding(end = MaterialTheme.spacing.base),
            icon = if (!isAttachmentVisible) {
                Icons.Filled.AttachFile
            } else {
                Icons.Filled.Attachment
            }
        )

        MessageField(
            messageText = value,
            onMessageTextChanged = onValueChange,
            isInputEnabled = inputEnabled,
            onTextLineCountChanged = { count -> textLineCount = count },
            modifier = Modifier.weight(1F)
        )

        SendButton(
            buttonWidth = buttonWidth,
            buttonHeight = buttonHeight,
            isRound = isMultiline,
            onSendClick = onSendClick,
            enabled = sendEnabled && (value.isNotBlank() || hasAttachments),
            isEditing = isEditing,
            modifier = Modifier.align(
                if (isMultiline) {
                    Alignment.Bottom
                } else {
                    Alignment.CenterVertically
                }
            )
        )
        VoiceButton(
            buttonHeight = buttonHeight,
            onVoiceClick = onVoiceClick,
            enabled = !isEditing,
            modifier = Modifier.align(Alignment.Bottom)
        )
    }
}

@Composable
private fun MessageField(
    messageText: String,
    onMessageTextChanged: (String) -> Unit,
    isInputEnabled: Boolean,
    onTextLineCountChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = messageText,
        onValueChange = onMessageTextChanged,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(
                horizontal = MaterialTheme.spacing.base,
                vertical = MaterialTheme.spacing.micro
            ),
        enabled = isInputEnabled,
        minLines = 1,
        maxLines = 5,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions =
            KeyboardOptions(
                imeAction = ImeAction.Default
            ),
        onTextLayout = { result ->
            onTextLineCountChanged(result.lineCount)
        },
        decorationBox = { innerTextField ->
            innerTextField()
        }
    )
}

@Composable
internal fun VoiceButton(
    buttonHeight: Dp,
    onVoiceClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onVoiceClick,
        enabled = enabled,
        modifier = modifier
            .padding(start = MaterialTheme.spacing.base)
            .requiredSize(buttonHeight),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.MessageInput.iconSize)
        )
    }
}

@Preview
@Composable
private fun MessageInputPreview() {
    SparrowTheme {
        MessageInput(
            value = "Hello",
            onValueChange = {},
            onSendClick = {},
            onVoiceClick = {},
            inputEnabled = true,
            sendEnabled = true,
            hasAttachments = false,
            onAttachmentClick = {},
            isAttachmentVisible = false
        )
    }
}

@Preview
@Composable
private fun MultilineMessageInputPreview() {
    SparrowTheme {
        MessageInput(
            value =
                "Hello, this is a longer message that wraps " +
                    "onto a second line.",
            onValueChange = {},
            onSendClick = {},
            onVoiceClick = {},
            onAttachmentClick = {},
            isAttachmentVisible = true,
            inputEnabled = true,
            sendEnabled = true,
            hasAttachments = false
        )
    }
}
