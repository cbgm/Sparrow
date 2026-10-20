package com.cbgm.sparrow.feature.identity.presentation.create.model

sealed interface IdentityCreateUiEvent {
    data class PhoneNumberChanged(
        val value: String
    ) : IdentityCreateUiEvent

    data class NameChanged(
        val value: String
    ) : IdentityCreateUiEvent

    data object CreateClicked : IdentityCreateUiEvent
}
