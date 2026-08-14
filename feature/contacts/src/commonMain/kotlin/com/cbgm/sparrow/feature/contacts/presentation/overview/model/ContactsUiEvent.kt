package com.cbgm.sparrow.feature.contacts.presentation.overview.model

sealed interface ContactsUiEvent {
    data class SearchQueryChanged(
        val query: String
    ) : ContactsUiEvent

    data object ImportDeviceContacts : ContactsUiEvent

    data object DeviceContactsPermissionDenied : ContactsUiEvent

    data object BackClicked : ContactsUiEvent

    data object ImportContactClicked : ContactsUiEvent

    data object CreateGroupClicked : ContactsUiEvent

    data class ContactClicked(
        val contactId: String,
        val contactName: String
    ) : ContactsUiEvent

    data class SelectionTitleChanged(
        val title: String
    ) : ContactsUiEvent

    data class ContactSelectionToggled(
        val contactId: String
    ) : ContactsUiEvent

    data object SelectionConfirmed : ContactsUiEvent
}
