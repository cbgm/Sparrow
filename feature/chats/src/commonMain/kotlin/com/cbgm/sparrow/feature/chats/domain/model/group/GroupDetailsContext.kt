package com.cbgm.sparrow.feature.chats.domain.model.group

data class GroupDetailsContext(
    val verification: GroupVerificationState,
    val administration: GroupAdministrationState,
    val conversation: GroupConversation?,
    val avatar: GroupAvatar
)
