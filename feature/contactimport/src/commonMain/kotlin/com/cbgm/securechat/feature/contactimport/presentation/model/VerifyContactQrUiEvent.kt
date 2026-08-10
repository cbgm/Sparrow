package com.cbgm.securechat.feature.contactimport.presentation.model

sealed interface VerifyContactQrUiEvent {
    data class QrCodeScanned(
        val encodedIdentity: String
    ) : VerifyContactQrUiEvent

    data object BackClicked : VerifyContactQrUiEvent

    data object ErrorDismissed : VerifyContactQrUiEvent
}
