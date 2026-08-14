package com.cbgm.sparrow.feature.contactimport.presentation.verify.model

sealed interface VerifyContactQrUiEvent {
    data class QrCodeScanned(
        val encodedIdentity: String
    ) : VerifyContactQrUiEvent

    data object BackClicked : VerifyContactQrUiEvent

    data object ErrorDismissed : VerifyContactQrUiEvent
}
