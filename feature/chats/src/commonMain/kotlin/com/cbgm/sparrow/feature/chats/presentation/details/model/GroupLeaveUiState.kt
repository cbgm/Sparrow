package com.cbgm.sparrow.feature.chats.presentation.details.model

enum class GroupLeavePrompt {
    CONFIRM,
    PROMOTE_ADMIN
}

data class GroupLeaveUiState(
    val prompt: GroupLeavePrompt? = null,
    val isLeaving: Boolean = false,
    val isLeaveRequested: Boolean = false,
    val errorMessage: String? = null
)
