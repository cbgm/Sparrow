package com.cbgm.sparrow.feature.chats.presentation.forwarding.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingTargetUi

@Composable
internal fun ForwardingTargetItem(
    target: ForwardingTargetUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.screenPadding,
                    vertical = MaterialTheme.spacing.micro
                )
                .clickable(onClick = onClick),
        leadingContent = {
            SparrowAvatar(
                name = target.displayName,
                target = target.avatarTarget
            )
        },
        headlineContent = {
            Text(
                text = target.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    )
}
