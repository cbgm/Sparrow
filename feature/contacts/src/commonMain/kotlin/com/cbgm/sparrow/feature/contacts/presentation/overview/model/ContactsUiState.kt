package com.cbgm.sparrow.feature.contacts.presentation.overview.model

sealed interface ContactsUiState {
    data object Loading : ContactsUiState

    data object Empty : ContactsUiState

    data class Content(
        val groups: List<ContactGroupEntity>,
        val profilePictures: Map<String, ByteArray?> = emptyMap()
    ) : ContactsUiState

    data class Error(
        val message: String
    ) : ContactsUiState
}
