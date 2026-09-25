package com.cbgm.sparrow.feature.chats.presentation.common.composer.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_composer_placeholder
import org.jetbrains.compose.resources.stringResource

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
            .padding(vertical = MaterialTheme.spacing.micro),
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
        RoundedInputButton(
            onClick = onVoiceClick,
            enabled = !isEditing,
            icon = Icons.Default.Mic,
            modifier = Modifier.padding(start = MaterialTheme.spacing.base)
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
            .heightIn(
                min = Dimens.MessageInput.composerHeight,
                max = Dimens.MessageInput.messageFieldHeightMax
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium
            )
            .border(
                width = Dimens.Base.borderStrokeWidth,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(
                horizontal = MaterialTheme.spacing.small,
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
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (messageText.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.feature_chats_composer_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        }
    )
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
