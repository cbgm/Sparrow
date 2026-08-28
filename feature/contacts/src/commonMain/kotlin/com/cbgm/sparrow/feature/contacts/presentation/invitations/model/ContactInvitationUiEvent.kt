package com.cbgm.sparrow.feature.contacts.presentation.invitations.model

sealed interface ContactInvitationUiEvent {
    data object CloseClicked : ContactInvitationUiEvent

    data class TabSelected(
        val tab: ContactInvitationTab
    ) : ContactInvitationUiEvent

    data class AcceptClicked(
        val invitationId: String
    ) : ContactInvitationUiEvent

    data class DeclineClicked(
        val invitationId: String
    ) : ContactInvitationUiEvent

    data class DeclineAndBlockClicked(
        val invitationId: String
    ) : ContactInvitationUiEvent

    data class DeleteDeclinedOutgoingClicked(
        val invitationId: String
    ) : ContactInvitationUiEvent
}
