package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.helper.darker
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_reply
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MessageContextActionMenu(
    color: Color,
    onReplyClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onCopyClick: () -> Unit
) {
    Surface(
        modifier = Modifier.width(200.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = color.darker(0.9f),
        shadowElevation = Dimens.ActionMenu.shadowElevation
    ) {
        Column {
            MessageReactionItem(onReactionClick = onReactionClick)

            HorizontalDivider(thickness = Dimens.Base.dividerThickness)

            MessageActionItem(
                text = stringResource(Res.string.feature_chats_reply),
                onClick = onReplyClick,
                icon = Icons.Default.Reply
            )

            HorizontalDivider(thickness = Dimens.Base.dividerThickness)

            MessageActionItem(
                text = "Copy",
                onClick = onCopyClick,
                Icons.Default.ContentCopy
            )
        }
    }
}

@Composable
private fun MessageReactionItem(onReactionClick: (String) -> Unit) {
    Row(
        modifier =
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(
                    horizontal = MaterialTheme.spacing.base,
                    vertical = MaterialTheme.spacing.micro
                ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
    ) {
        listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { emoji ->
            Text(
                text = emoji,
                modifier = Modifier
                    .clickable { onReactionClick(emoji) }
                    .padding(MaterialTheme.spacing.micro),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun MessageActionItem(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector
) {
    Row(
        modifier =
            Modifier
                .height(Dimens.ActionMenu.actionItemHeight)
                .clickable(onClick = onClick)
                .padding(horizontal = MaterialTheme.spacing.actionItem.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Dimens.MessageBubble.iconSize),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview
@Composable
private fun MessagePopUpMenuPreview() {
    SparrowTheme {
        MessageContextActionMenu(
            color = MaterialTheme.colorScheme.surface,
            onReplyClick = {},
            onReactionClick = {},
            onCopyClick = {}
        )
    }
}
