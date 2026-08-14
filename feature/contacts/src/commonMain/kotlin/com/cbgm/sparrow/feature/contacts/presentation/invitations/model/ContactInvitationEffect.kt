package com.cbgm.sparrow.feature.contacts.presentation.invitations.model

sealed interface ContactInvitationEffect {
    data class ShowError(
        val message: String
    ) : ContactInvitationEffect
}
