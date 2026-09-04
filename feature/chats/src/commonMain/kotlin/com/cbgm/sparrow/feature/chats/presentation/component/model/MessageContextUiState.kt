package com.cbgm.sparrow.feature.chats.presentation.component.model

data class MessageContextUiState<T>(
    val message: T? = null,
    val canEdit: Boolean = false
)
