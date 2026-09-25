package com.cbgm.sparrow.feature.chats.presentation.details.model

data class GroupTitleUiState(
    val title: String = "",
    val canEdit: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
