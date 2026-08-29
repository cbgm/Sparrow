package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.feature.safety.presentation.component.MessageSafetyWarning
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

@Composable
internal fun TextMessageBubbleBody(
    textPart: MessagePartUi.Text,
    safetyWarning: MessageSafetyWarningUi?,
    onSafetyDetailsClick: () -> Unit
) {
    val annotatedText = rememberLinkAnnotatedString(textPart.text)

    Column {
        if (textPart.text.isNotBlank() || textPart.isContentFailed) {
            Row(modifier = Modifier.padding(MaterialTheme.spacing.micro)) {
                if (textPart.isContentFailed) {
                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.base))
                }

                Text(
                    text = annotatedText,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        safetyWarning?.let { warning ->
            MessageSafetyWarning(
                warning = warning,
                onClick = onSafetyDetailsClick
            )
        }
    }
}

@Preview
@Composable
private fun TextMessageBubbleBodyPreview() {
    SparrowTheme {
        TextMessageBubbleBody(
            textPart = MessagePartUi.Text(
                text = "Encrypted message",
                isContentFailed = false
            ),
            safetyWarning = null,
            onSafetyDetailsClick = {}
        )
    }
}
