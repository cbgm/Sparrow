package com.cbgm.sparrow.feature.contactimport.presentation.scan.model

sealed interface ScanIdentityUiEvent {
    data class QrCodeScanned(
        val encodedIdentity: String
    ) : ScanIdentityUiEvent

    data object BackClicked : ScanIdentityUiEvent
}
