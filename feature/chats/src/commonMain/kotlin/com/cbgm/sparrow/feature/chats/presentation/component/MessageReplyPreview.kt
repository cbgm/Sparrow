package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Reply
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
internal fun MessageReplyPreview(
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
        ReplyPreviewContent(reply = reply, isMine = isMine)
    }
}

@Composable
internal fun ComposerReplyPreview(
    reply: MessageReplyUi,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.base,
                vertical = MaterialTheme.spacing.micro
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        ComposerReplyPreviewContent(
            reply = reply,
            modifier = Modifier.weight(1f)
        )

        Surface(
            modifier = Modifier
                .size(Dimens.MediaSelection.previewRemoveButtonSize)
                .clickable(onClick = onCancel),
            shape = CircleShape,
            border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.primary),
            color = Color.Transparent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.MediaSelection.previewRemoveIconSize),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ReplyPreviewContent(
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
            imageVector = Icons.Default.Reply,
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
private fun ComposerReplyPreviewContent(
    reply: MessageReplyUi,
    modifier: Modifier = Modifier
) {
    val (sender, previewText) = rememberReplyPreviewData(reply)

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
        ) {
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = null,
                modifier = Modifier.size(Dimens.MessageBubble.iconSize),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = sender,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary
            )
        }
        previewText?.takeIf(String::isNotBlank)?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
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
private fun MessageReplyPreviewPreview() {
    SparrowTheme {
        MessageReplyPreview(
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

@Preview
@Composable
private fun ComposerReplyPreviewPreview() {
    SparrowTheme {
        ComposerReplyPreview(
            reply =
                MessageReplyUi(
                    messageId = "message-1",
                    isMine = true,
                    previewText = "The original message should only appear as a short excerpt in the composer"
                ),
            onCancel = {}
        )
    }
}
