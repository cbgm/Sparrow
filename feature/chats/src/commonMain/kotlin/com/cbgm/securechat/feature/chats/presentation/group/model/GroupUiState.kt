package com.cbgm.securechat.feature.chats.presentation.group.model

import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.GroupConversationState
import com.cbgm.securechat.feature.chats.domain.model.GroupMemberInvitationStatus

data class GroupMemberProgressUi(
    val displayName: String,
    val status: GroupMemberInvitationStatus
)

data class GroupUiState(
    val title: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val messageText: String = "",
    val isSomeoneTyping: Boolean = false,
    val typingDisplayName: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = true,
    val isMessageInputEnabled: Boolean = false,
    val state: GroupConversationState = GroupConversationState.READY,
    val memberCount: Int = 0,
    val readyMemberCount: Int = 0,
    val pendingMemberCount: Int = 0,
    val showInvitationActions: Boolean = false,
    val memberProgress: List<GroupMemberProgressUi> = emptyList()
)
