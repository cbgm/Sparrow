package com.cbgm.sparrow.feature.contacts.presentation.blocklist.model

sealed interface BlockedContactsEffect {
    data class ShowError(
        val message: String
    ) : BlockedContactsEffect
}
