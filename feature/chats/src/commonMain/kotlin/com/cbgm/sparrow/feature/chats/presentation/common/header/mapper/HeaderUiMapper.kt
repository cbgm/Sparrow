package com.cbgm.sparrow.feature.chats.presentation.common.header.mapper

import com.cbgm.sparrow.feature.chats.presentation.common.header.model.HeaderAvatarKind
import com.cbgm.sparrow.feature.chats.presentation.common.header.model.HeaderUiModel
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState

internal fun DirectConversationUiState.toHeaderUiModel() = HeaderUiModel(
    title = contactName,
    avatarId = contactId,
    avatarKind = HeaderAvatarKind.USER
)

internal fun GroupConversationUiState.toHeaderUiModel(subtitle: String) = HeaderUiModel(
    title = title,
    avatarId = groupId,
    avatarKind = HeaderAvatarKind.GROUP,
    subtitle = subtitle
)
