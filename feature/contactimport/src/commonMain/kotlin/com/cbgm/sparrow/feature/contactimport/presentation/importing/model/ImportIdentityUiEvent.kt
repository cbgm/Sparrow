package com.cbgm.sparrow.feature.contactimport.presentation.importing.model

sealed interface ImportIdentityUiEvent {
    data class EncodedIdentityChanged(
        val value: String
    ) : ImportIdentityUiEvent

    data object ImportClicked : ImportIdentityUiEvent

    data object ScannedIdentityConfirmed : ImportIdentityUiEvent

    data object ScannedIdentityDismissed : ImportIdentityUiEvent

    data object BackClicked : ImportIdentityUiEvent

    data object ScanQrCodeClicked : ImportIdentityUiEvent
}
