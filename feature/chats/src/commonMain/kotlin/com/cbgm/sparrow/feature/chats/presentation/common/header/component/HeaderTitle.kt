package com.cbgm.sparrow.feature.chats.presentation.common.header.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.chats.presentation.common.header.model.HeaderAvatarKind
import com.cbgm.sparrow.feature.chats.presentation.common.header.model.HeaderUiModel

@Composable
internal fun HeaderTitle(model: HeaderUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarTarget = model.avatarId.takeIf(String::isNotBlank)?.let { id ->
            when (model.avatarKind) {
                HeaderAvatarKind.USER -> AvatarTarget.User(id)
                HeaderAvatarKind.GROUP -> AvatarTarget.Group(id)
            }
        }
        val avatarSize = when (model.avatarKind) {
            HeaderAvatarKind.USER -> Dimens.DirectConversationScreen.topBarAvatarSize
            HeaderAvatarKind.GROUP -> Dimens.GroupConversationScreen.topBarAvatarSize
        }
        SparrowAvatar(
            name = model.title,
            target = avatarTarget,
            size = avatarSize
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        if (model.subtitle != null) {
            Column {
                Text(
                    text = model.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = model.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = Alpha.OpaqueText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Text(
                text = model.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
