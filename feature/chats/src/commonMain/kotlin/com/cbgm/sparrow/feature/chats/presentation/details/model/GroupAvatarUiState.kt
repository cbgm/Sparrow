package com.cbgm.sparrow.feature.chats.presentation.details.model

data class GroupAvatarUiState(
    val title: String = "",
    val avatarBytes: ByteArray? = null,
    val canEdit: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
