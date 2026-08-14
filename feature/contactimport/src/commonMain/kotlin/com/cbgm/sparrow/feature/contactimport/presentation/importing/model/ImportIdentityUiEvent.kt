package com.cbgm.sparrow.feature.contactimport.presentation.importing.model

import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust

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
