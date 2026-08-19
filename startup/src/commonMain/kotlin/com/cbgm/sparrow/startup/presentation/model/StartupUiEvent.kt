package com.cbgm.sparrow.startup.presentation.model

sealed interface StartupUiEvent {
    data object RequestPhoneNumberHint : StartupUiEvent

    data class PhoneNumberChanged(
        val value: String
    ) : StartupUiEvent

    data object CreateIdentityClicked : StartupUiEvent

    data object IdentityCreated : StartupUiEvent

    data object RetryClicked : StartupUiEvent
}
