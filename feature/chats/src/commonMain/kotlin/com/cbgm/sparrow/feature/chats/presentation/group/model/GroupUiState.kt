package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMemberInvitationStatus

data class GroupMemberProgressUi(
    val displayName: String,
    val status: GroupMemberInvitationStatus
)

data class GroupUiState(
    val title: String = "",
    val avatarBytes: ByteArray? = null,
    val messages: List<GroupMessageUiModel> = emptyList(),
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
