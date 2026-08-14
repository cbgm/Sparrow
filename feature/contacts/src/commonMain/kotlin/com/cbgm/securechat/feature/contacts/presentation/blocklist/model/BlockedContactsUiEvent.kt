package com.cbgm.securechat.feature.contacts.presentation.blocklist.model

sealed interface BlockedContactsUiEvent {
    data object BackClicked : BlockedContactsUiEvent

    data object AddContactClicked : BlockedContactsUiEvent

    data object AddContactsDismissed : BlockedContactsUiEvent

    data class PhoneNumberChanged(
        val value: String
    ) : BlockedContactsUiEvent

    data object BlockPhoneNumberClicked : BlockedContactsUiEvent

    data class BlockContactClicked(
        val contactId: String
    ) : BlockedContactsUiEvent

    data class UnblockContactClicked(
        val contactId: String
    ) : BlockedContactsUiEvent
}
