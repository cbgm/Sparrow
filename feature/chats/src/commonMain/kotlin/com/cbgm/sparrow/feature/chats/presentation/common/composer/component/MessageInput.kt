package com.cbgm.sparrow.feature.chats.presentation.common.composer.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Alpha
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
    val canSend = sendEnabled && (value.isNotBlank() || hasAttachments)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.micro),
        verticalAlignment = Alignment.Bottom
    ) {
        ComposerButton(
            onClick = onAttachmentClick,
            enabled = attachmentsEnabled,
            icon = if (!isAttachmentVisible) {
                SparrowIcons.Attach
            } else {
                SparrowIcons.AttachOpen
            }
        )

        MessageField(
            messageText = value,
            onMessageTextChanged = onValueChange,
            isInputEnabled = inputEnabled,
            modifier = Modifier.weight(1F)
        )

        AnimatedVisibility(visible = canSend || isEditing) {
            ComposerButton(
                onClick = onSendClick,
                enabled = canSend,
                icon = if (isEditing) SparrowIcons.Check else SparrowIcons.Send,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        ComposerButton(
            onClick = onVoiceClick,
            enabled = !isEditing,
            icon = SparrowIcons.Mic
        )
    }
}

@Composable
private fun MessageField(
    messageText: String,
    onMessageTextChanged: (String) -> Unit,
    isInputEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = messageText,
        onValueChange = onMessageTextChanged,
        modifier = modifier
            .heightIn(
                min = Dimens.MessageInput.actionSize,
                max = Dimens.MessageInput.messageFieldHeightMax
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
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (messageText.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.feature_chats_composer_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.MessageInput.placeholder)
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
