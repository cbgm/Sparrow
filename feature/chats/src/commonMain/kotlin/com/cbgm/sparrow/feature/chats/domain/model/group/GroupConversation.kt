package com.cbgm.sparrow.feature.chats.domain.model.group

import com.cbgm.sparrow.feature.membership.domain.model.GroupConversationState
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberProgress

data class GroupConversation(
    val id: String,
    val title: String,
    val messages: List<GroupMessage>,
    val unreadCount: Int,
    val participantContactIds: List<String>,
    val pendingParticipantCount: Int,
    val isReady: Boolean,
    val state: GroupConversationState,
    val memberProgress: List<GroupMemberProgress>
)
