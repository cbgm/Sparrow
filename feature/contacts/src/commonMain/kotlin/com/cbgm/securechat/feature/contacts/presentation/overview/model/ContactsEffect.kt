package com.cbgm.securechat.feature.contacts.presentation.overview.model

sealed interface ContactsEffect {
    data class ShowError(
        val message: String
    ) : ContactsEffect

    data object BackRequested : ContactsEffect

    data object ImportContactRequested : ContactsEffect

    data object CreateGroupRequested : ContactsEffect

    data class ContactSelected(
        val contactId: String,
        val contactName: String
    ) : ContactsEffect
}
