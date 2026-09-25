package com.cbgm.sparrow.feature.chats.presentation.details.model

data class GroupAvatarUiState(
    val groupId: String = "",
    val title: String = "",
    val hasAvatar: Boolean = false,
    val canEdit: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
