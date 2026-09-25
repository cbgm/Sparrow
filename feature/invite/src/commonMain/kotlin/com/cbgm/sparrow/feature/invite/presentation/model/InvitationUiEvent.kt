package com.cbgm.sparrow.feature.invite.presentation.model

sealed interface InvitationUiEvent {
    data object CloseClicked : InvitationUiEvent

    data class TabSelected(
        val tab: InvitationTab
    ) : InvitationUiEvent

    data class AcceptClicked(
        val invitationId: String
    ) : InvitationUiEvent

    data class DeclineClicked(
        val invitationId: String
    ) : InvitationUiEvent

    data class DeclineAndBlockClicked(
        val invitationId: String
    ) : InvitationUiEvent

    data class DeleteDeclinedOutgoingClicked(
        val invitationId: String
    ) : InvitationUiEvent
}
