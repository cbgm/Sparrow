package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.helper.darker
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageReplyUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_attachment
import com.cbgm.sparrow.resources.feature_chats_original_message_unavailable
import com.cbgm.sparrow.resources.feature_chats_you
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MessageReplyInlay(
    reply: MessageReplyUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color,
    isMine: Boolean
) {
    val isAvailable = reply.isMine != null
    val clickModifier = if (isAvailable && onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier),
        color = color.darker(0.9f),
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Content(reply = reply, isMine = isMine)
    }
}

@Composable
private fun Content(
    reply: MessageReplyUi,
    modifier: Modifier = Modifier,
    isMine: Boolean
) {
    val (sender, previewText) = rememberReplyPreviewData(reply)

    val padding = remember(isMine) {
        if (isMine) {
            val left = 8.dp
            val right = 16.dp
            Pair(left, right)
        } else {
            val left = 16.dp
            val right = 8.dp
            Pair(left, right)
        }
    }

    Row(
        modifier = modifier.padding(
            start = padding.first,
            end = padding.second,
            top = MaterialTheme.spacing.micro,
            bottom = MaterialTheme.spacing.micro
        ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            modifier = Modifier.size(Dimens.MessageBubble.iconSize)
        )
        Text(
            text = sender,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        previewText?.takeIf(String::isNotBlank)?.let { text ->
            Text(
                text = " · $text",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun rememberReplyPreviewData(reply: MessageReplyUi): Pair<String, String?> {
    val isAvailable = reply.isMine != null
    val unavailableStr = stringResource(Res.string.feature_chats_original_message_unavailable)
    val youStr = stringResource(Res.string.feature_chats_you)
    val attachmentStr = stringResource(Res.string.feature_chats_attachment)

    return remember(reply, unavailableStr, youStr, attachmentStr) {
        val sender = when {
            !isAvailable -> unavailableStr
            reply.isMine -> youStr
            !reply.senderName.isNullOrBlank() -> reply.senderName
            else -> unavailableStr
        }
        val preview = if (isAvailable) {
            (reply.previewText ?: attachmentStr)
        } else {
            null
        }
        Pair(sender, preview)
    }
}

@Preview
@Composable
private fun MessageReplyPreview() {
    SparrowTheme {
        MessageReplyInlay(
            reply =
                MessageReplyUi(
                    messageId = "message-1",
                    isMine = false,
                    senderName = "Chris",
                    previewText =
                        "This is the original message and it is intentionally much longer than the reply preview should display"
                ),
            color = MaterialTheme.colorScheme.surfaceContainer,
            isMine = false
        )
    }
}
