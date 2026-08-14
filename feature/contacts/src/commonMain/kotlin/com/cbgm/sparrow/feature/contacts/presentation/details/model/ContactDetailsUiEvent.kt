package com.cbgm.sparrow.feature.contacts.presentation.details.model

sealed interface ContactDetailsUiEvent {
    data object BackClicked : ContactDetailsUiEvent

    data object RetryClicked : ContactDetailsUiEvent

    data object ShareContactClicked : ContactDetailsUiEvent

    data object VerifyIdentityClicked : ContactDetailsUiEvent

    data object ConfirmVerificationClicked : ContactDetailsUiEvent

    data object ScanQrCodeClicked : ContactDetailsUiEvent

    data object VerificationBackClicked : ContactDetailsUiEvent
}
