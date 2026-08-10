package com.cbgm.securechat.feature.contacts.presentation.model

sealed interface ContactInvitationUiEvent {
    data class AcceptClicked(
        val invitationId: String
    ) : ContactInvitationUiEvent

    data class DeclineClicked(
        val invitationId: String
    ) : ContactInvitationUiEvent

    data class DeclineAndBlockClicked(
        val invitationId: String
    ) : ContactInvitationUiEvent
}
