package com.cbgm.sparrow.feature.contacts.presentation.overview.model

sealed interface ContactsUiState {
    data object Loading : ContactsUiState

    data object Empty : ContactsUiState

    data class Content(
        val groups: List<ContactGroupEntity>
    ) : ContactsUiState

    data class Error(
        val message: String
    ) : ContactsUiState
}
