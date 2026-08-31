package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
internal fun MessageContextActionMenu(color: Color, onReplyClick: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.width(200.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = color.darker(0.9f),
        shadowElevation = 8.dp
    ) {
        Column {
            MessageActionItem(
                text = stringResource(Res.string.feature_chats_reply),
                onClick = {
                    onDismiss()
                    onReplyClick()
                },
                icon = Icons.Default.Reply
            )

            HorizontalDivider(thickness = 0.5.dp)

            MessageActionItem(
                text = "Copy",
                onClick = {
                    onDismiss()
                },
                Icons.Default.ContentCopy
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
                .height(36.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
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
            onDismiss = {},
            onReplyClick = {}
        )
    }
}
