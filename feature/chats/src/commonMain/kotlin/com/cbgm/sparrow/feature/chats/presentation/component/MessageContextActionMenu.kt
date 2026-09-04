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
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
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
import com.cbgm.sparrow.core.ui.helper.darker
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_copy
import com.cbgm.sparrow.resources.feature_chats_delete_message
import com.cbgm.sparrow.resources.feature_chats_edit_message
import com.cbgm.sparrow.resources.feature_chats_forward
import com.cbgm.sparrow.resources.feature_chats_reply
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MessageContextActionMenu(
    color: Color,
    onReplyClick: () -> Unit,
    onForwardClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    showEdit: Boolean,
    onEditClick: () -> Unit,
    onCopyClick: () -> Unit,
    showDelete: Boolean,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier.width(Dimens.ActionMenu.menuWidth),
        shape = MaterialTheme.shapes.extraSmall,
        color = color.darker(0.9f),
        shadowElevation = Dimens.ActionMenu.shadowElevation
    ) {
        Column {
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
                listOf("👍", "❤️", "😂", "🔥", "💯", "💀", "😮", "😢", "🙏", "🤦‍♂️", "🤯", "🤔")
                    .forEach { emoji ->
                        Text(
                            text = emoji,
                            modifier = Modifier
                                .clickable { onReactionClick(emoji) }
                                .padding(MaterialTheme.spacing.micro),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
            }

            HorizontalDivider(thickness = Dimens.Base.dividerThickness)

            MessageActionItem(
                text = stringResource(Res.string.feature_chats_reply),
                onClick = onReplyClick,
                icon = Icons.AutoMirrored.Filled.Reply
            )

            HorizontalDivider(thickness = Dimens.Base.dividerThickness)

            MessageActionItem(
                text = stringResource(Res.string.feature_chats_forward),
                onClick = onForwardClick,
                icon = Icons.AutoMirrored.Filled.Forward
            )

            if (showEdit) {
                HorizontalDivider(thickness = Dimens.Base.dividerThickness)

                MessageActionItem(
                    text = stringResource(Res.string.feature_chats_edit_message),
                    onClick = onEditClick,
                    icon = Icons.Default.Edit
                )
            }

            HorizontalDivider(thickness = Dimens.Base.dividerThickness)

            MessageActionItem(
                text = stringResource(Res.string.feature_chats_copy),
                onClick = onCopyClick,
                icon = Icons.Default.ContentCopy
            )

            if (showDelete) {
                HorizontalDivider(thickness = Dimens.Base.dividerThickness)

                MessageActionItem(
                    text = stringResource(Res.string.feature_chats_delete_message),
                    onClick = onDeleteClick,
                    icon = Icons.Default.DeleteOutline
                )
            }
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
            onForwardClick = {},
            onReactionClick = {},
            showEdit = true,
            onEditClick = {},
            onCopyClick = {},
            showDelete = true,
            onDeleteClick = {}
        )
    }
}
