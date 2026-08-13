package com.cbgm.securechat.feature.chats.presentation.details.model

data class GroupLeaveUiState(
    val isLeaving: Boolean = false,
    val isLeaveRequested: Boolean = false,
    val errorMessage: String? = null
)
