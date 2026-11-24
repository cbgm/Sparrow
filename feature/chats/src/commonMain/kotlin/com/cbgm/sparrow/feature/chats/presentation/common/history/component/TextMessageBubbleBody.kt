package com.cbgm.sparrow.feature.chats.presentation.common.history.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import com.cbgm.sparrow.core.base.messagepart.ui.model.TextUi
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.linkpreview.presentation.component.LinkPreview
import com.cbgm.sparrow.feature.linkpreview.presentation.model.TextContentPart
import com.cbgm.sparrow.feature.linkpreview.presentation.model.toTextContentParts
import com.cbgm.sparrow.feature.safety.presentation.component.MessageSafetyWarning
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

@Composable
internal fun TextMessageBubbleBody(
    textPart: TextUi,
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
                    parts = textPart.text.toTextContentParts(),
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

/** Room the bubble keeps for its time and delivery at the end of the last line of text; null when they go elsewhere. */
internal val LocalTrailingMetadataSize = staticCompositionLocalOf<DpSize?> { null }

@Composable
private fun TextContent(
    parts: List<TextContentPart>,
    modifier: Modifier = Modifier
) {
    val trailingRoom = LocalTrailingMetadataSize.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        parts.forEachIndexed { index, part ->
            when (part) {
                is TextContentPart.Text ->
                    NonEmptyTextWithRoom(text = part.text, room = trailingRoom.takeIf { index == parts.lastIndex })

                is TextContentPart.LinkPreview ->
                    LinkPreview(url = part.url)
            }
        }
    }
}

/** The text, with an invisible [room] after its last word that wraps to a new line like a word when it does not fit. */
@Composable
private fun NonEmptyTextWithRoom(
    text: String,
    room: DpSize?
) {
    if (text.isNotEmpty()) TextWithRoom(text = text, room = room)
}

@Composable
private fun TextWithRoom(
    text: String,
    room: DpSize?
) {
    if (room == null) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
        return
    }
    val density = LocalDensity.current
    val placeholder =
        with(density) { Placeholder(width = room.width.toSp(), height = room.height.toSp(), placeholderVerticalAlign = PlaceholderVerticalAlign.TextBottom) }
    Text(
        text =
            buildAnnotatedString {
                append(text)
                appendInlineContent(TRAILING_ROOM)
            },
        inlineContent = mapOf(TRAILING_ROOM to InlineTextContent(placeholder) {}),
        style = MaterialTheme.typography.bodyMedium
    )
}

private const val TRAILING_ROOM = "trailingRoom"

@Composable
private fun FailedTextContent(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
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
                TextUi(
                    id = "preview-text",
                    text = "Encrypted message",
                    isContentFailed = false
                ),
            safetyWarning = null,
            onSafetyDetailsClick = {}
        )
    }
}
