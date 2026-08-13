package com.cbgm.securechat.feature.identity.presentation.setup.model

sealed interface IdentityUiEvent {
    data object RequestPhoneNumberHint : IdentityUiEvent

    data class PhoneNumberChanged(
        val value: String
    ) : IdentityUiEvent

    data class NameChanged(
        val value: String
    ) : IdentityUiEvent

    data object CreateIdentityClicked : IdentityUiEvent

    data object RetryClicked : IdentityUiEvent

    data object ShareIdentityClicked : IdentityUiEvent
}
