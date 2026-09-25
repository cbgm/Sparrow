package com.cbgm.sparrow.feature.autoreply.presentation.model

data class AutoReplyUiState(
    val replies: List<AutoReplyUiItem> = emptyList(),
    val editor: AutoReplyEditorUiState? = null,
    val isSaving: Boolean = false
)

data class AutoReplyUiItem(
    val id: String,
    val name: String,
    val text: String,
    val isActive: Boolean
)

data class AutoReplyEditorUiState(
    val id: String? = null,
    val name: String = "",
    val text: String = ""
) {
    val isEditing: Boolean
        get() = id != null

    val canSave: Boolean
        get() = name.isNotBlank() && text.isNotBlank()
}
