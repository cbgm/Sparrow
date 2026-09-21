package com.cbgm.sparrow.feature.chats.presentation.common.history.component

import androidx.compose.foundation.layout.Arrangement
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
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessagePartUi
import com.cbgm.sparrow.feature.linkpreview.presentation.component.LinkPreview
import com.cbgm.sparrow.feature.linkpreview.presentation.model.TextContentPart
import com.cbgm.sparrow.feature.safety.presentation.component.MessageSafetyWarning
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

@Composable
internal fun TextMessageBubbleBody(
    textPart: MessagePartUi.Text,
    safetyWarning: MessageSafetyWarningUi?,
    onSafetyDetailsClick: () -> Unit
) {
    val padding =
        if (safetyWarning != null) MaterialTheme.spacing.base else MaterialTheme.spacing.micro

    Column {
        if (textPart.text.isNotBlank() || textPart.isContentFailed) {
            if (textPart.isContentFailed) {
                FailedTextContent(
                    text = textPart.text,
                    modifier = Modifier.padding(padding)
                )
            } else {
                TextContent(
                    parts = textPart.contentParts,
                    modifier = Modifier.padding(padding)
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

@Composable
private fun TextContent(
    parts: List<TextContentPart>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        parts.forEach { part ->
            when (part) {
                is TextContentPart.Text ->
                    if (part.text.isNotEmpty()) {
                        Text(
                            text = part.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                is TextContentPart.LinkPreview ->
                    LinkPreview(url = part.url)
            }
        }
    }
}

@Composable
private fun FailedTextContent(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.base))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview
@Composable
private fun TextMessageBubbleBodyPreview() {
    SparrowTheme {
        TextMessageBubbleBody(
            textPart =
                MessagePartUi.Text(
                    text = "Encrypted message",
                    isContentFailed = false
                ),
            safetyWarning = null,
            onSafetyDetailsClick = {}
        )
    }
}
