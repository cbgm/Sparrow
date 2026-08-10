package com.cbgm.securechat.feature.contacts.presentation.model

sealed interface ContactInvitationEffect {
    data class ShowError(
        val message: String
    ) : ContactInvitationEffect
}
