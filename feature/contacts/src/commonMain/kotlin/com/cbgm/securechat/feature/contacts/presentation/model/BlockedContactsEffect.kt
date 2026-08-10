package com.cbgm.securechat.feature.contacts.presentation.model

sealed interface BlockedContactsEffect {
    data class ShowError(
        val message: String
    ) : BlockedContactsEffect
}
