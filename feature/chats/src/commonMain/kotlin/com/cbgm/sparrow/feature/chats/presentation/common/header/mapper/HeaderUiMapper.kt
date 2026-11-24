package com.cbgm.sparrow.feature.chats.presentation.common.header.mapper

import com.cbgm.sparrow.feature.avatar.presentation.model.AvatarRing
import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
import com.cbgm.sparrow.feature.chats.presentation.common.header.model.HeaderAvatarKind
import com.cbgm.sparrow.feature.chats.presentation.common.header.model.HeaderUi
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState

internal fun DirectConversationUiState.toHeaderUi() = HeaderUi(
    title = contactName,
    avatarId = contactId,
    avatarKind = HeaderAvatarKind.USER,
    ring =
        if (contactSecurityState == ContactSecurityState.MUTUAL_KEYS_VERIFIED) {
            AvatarRing.VERIFIED
        } else {
            AvatarRing.PENDING
        }
)

internal fun GroupConversationUiState.toHeaderUi(subtitle: String) = HeaderUi(
    title = title,
    avatarId = groupId,
    avatarKind = HeaderAvatarKind.GROUP,
    subtitle = subtitle
)
