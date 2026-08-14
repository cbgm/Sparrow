package com.cbgm.securechat.startup.presentation.model

sealed interface StartupUiEvent {
    data object RequestPhoneNumberHint : StartupUiEvent

    data class PhoneNumberChanged(
        val value: String
    ) : StartupUiEvent

    data object CreateIdentityClicked : StartupUiEvent

    data object RetryClicked : StartupUiEvent
}
