package com.cbgm.sparrow.feature.identity.presentation.verification.model

sealed interface PhoneVerificationUiEvent {
    data class CodeChanged(
        val value: String
    ) : PhoneVerificationUiEvent

    data object ConfirmClicked : PhoneVerificationUiEvent

    data object ResendClicked : PhoneVerificationUiEvent

    data object SkipClicked : PhoneVerificationUiEvent

    data object DismissClicked : PhoneVerificationUiEvent
}
