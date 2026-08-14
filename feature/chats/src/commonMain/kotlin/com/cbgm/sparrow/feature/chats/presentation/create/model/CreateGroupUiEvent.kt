package com.cbgm.sparrow.feature.chats.presentation.create.model

sealed interface CreateGroupUiEvent {
    data object BackClicked : CreateGroupUiEvent

    data class TitleChanged(
        val title: String
    ) : CreateGroupUiEvent

    data class SearchQueryChanged(
        val query: String
    ) : CreateGroupUiEvent

    data class ContactSelected(
        val contactId: String
    ) : CreateGroupUiEvent

    data object CreateClicked : CreateGroupUiEvent
}
