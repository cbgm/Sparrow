package com.cbgm.sparrow.feature.chats.presentation.details.model

data class GroupVerificationUiState(
    val summary: GroupVerificationSummaryUiState = GroupVerificationSummaryUiState(),
    val selectedMember: GroupMemberVerificationUiState? = null,
    val safetyNumber: String = "",
    val isLoadingSafetyNumber: Boolean = false,
    val isVerifying: Boolean = false,
    val errorMessage: String? = null,
    val groupAvatar: GroupAvatarUiState = GroupAvatarUiState(),
    val memberManagement: GroupMemberManagementUiState = GroupMemberManagementUiState(),
    val leave: GroupLeaveUiState = GroupLeaveUiState()
)
