package com.cbgm.securechat.feature.contactimport.presentation.model

sealed interface ScanIdentityUiEvent {
    data class QrCodeScanned(
        val encodedIdentity: String
    ) : ScanIdentityUiEvent

    data object BackClicked : ScanIdentityUiEvent
}
