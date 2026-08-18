package com.cbgm.sparrow.feature.contacts.presentation.overview.model

sealed interface ContactsUiState {
    val searchQuery: String

    data class Loading(
        override val searchQuery: String = ""
    ) : ContactsUiState

    data class Empty(
        override val searchQuery: String = ""
    ) : ContactsUiState

    data class Content(
        val groups: List<ContactGroupEntity>,
        val profilePictures: Map<String, ByteArray?> = emptyMap(),
        override val searchQuery: String = ""
    ) : ContactsUiState

    data class Error(
        val message: String,
        override val searchQuery: String = ""
    ) : ContactsUiState
}
