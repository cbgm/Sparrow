package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMemberInvitationStatus

data class GroupMemberProgressUi(
    val displayName: String,
    val status: GroupMemberInvitationStatus
)

data class GroupMembershipUiState(
    val memberCount: Int = 0,
    val readyMemberCount: Int = 0,
    val pendingMemberCount: Int = 0,
    val memberProgress: List<GroupMemberProgressUi> = emptyList()
)
