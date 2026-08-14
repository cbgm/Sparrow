package com.cbgm.securechat.feature.chats.presentation.create.model

sealed interface ContactsFlowUiEvent {
    data class ContactSelected(
        val contactId: String,
        val contactName: String
    ) : ContactsFlowUiEvent

    data object ImportContactClicked : ContactsFlowUiEvent

    data class GroupCreated(
        val conversationId: String
    ) : ContactsFlowUiEvent
}
