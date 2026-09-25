package com.cbgm.sparrow.feature.chats.presentation.common.history.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.presentation.component.SparrowAvatar
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageBubbleUi

@Composable
internal fun HistorySenderAvatar(message: MessageBubbleUi) {
    Surface(
        modifier = Modifier.padding(end = MaterialTheme.spacing.groupConversationScreen.senderGap),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        SparrowAvatar(
            name = message.senderName.orEmpty(),
            target = message.groupExtension?.senderContactId
                ?.takeIf(String::isNotBlank)
                ?.let { AvatarTarget.User(it) },
            size = Dimens.GroupConversationScreen.avatarSize
        )
    }
}
