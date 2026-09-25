package com.cbgm.sparrow.feature.autoreply.presentation.model

sealed interface AutoReplyUiEvent {
    data object BackClicked : AutoReplyUiEvent

    data object AddClicked : AutoReplyUiEvent

    data object OffClicked : AutoReplyUiEvent

    data class ActivateClicked(
        val id: String
    ) : AutoReplyUiEvent

    data class EditClicked(
        val id: String
    ) : AutoReplyUiEvent

    data class DeleteClicked(
        val id: String
    ) : AutoReplyUiEvent

    data class EditorNameChanged(
        val value: String
    ) : AutoReplyUiEvent

    data class EditorTextChanged(
        val value: String
    ) : AutoReplyUiEvent

    data object EditorSaveClicked : AutoReplyUiEvent

    data object EditorDismissed : AutoReplyUiEvent
}

sealed interface AutoReplyEffect {
    data class ShowSnackbar(
        val message: String
    ) : AutoReplyEffect
}
