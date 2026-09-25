package com.cbgm.sparrow.feature.invite.presentation.model

sealed interface InvitationEffect {
    data class ShowError(
        val message: String
    ) : InvitationEffect
}
