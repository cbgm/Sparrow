package com.cbgm.securechat.feature.contactimport.presentation.importing.model

import com.cbgm.securechat.feature.contacts.domain.model.IdentityImportTrust

sealed interface ImportIdentityUiEvent {
    data class EncodedIdentityChanged(
        val value: String
    ) : ImportIdentityUiEvent

    data class ImportClicked(
        val contactId: String?,
        val identityImportTrust: IdentityImportTrust
    ) : ImportIdentityUiEvent

    data object BackClicked : ImportIdentityUiEvent

    data object ScanQrCodeClicked : ImportIdentityUiEvent
}
