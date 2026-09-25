package com.cbgm.sparrow.feature.chats.domain.model.group

import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState

data class GroupDetailsContext(
    val verification: GroupVerificationState,
    val administration: GroupAdministrationState,
    val conversation: GroupConversation?,
    val avatarMetadata: GroupAvatarMetadata,
    val description: GroupDescription
)
