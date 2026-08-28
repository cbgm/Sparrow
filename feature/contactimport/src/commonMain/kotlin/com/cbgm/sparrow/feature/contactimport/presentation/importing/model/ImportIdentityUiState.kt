package com.cbgm.sparrow.feature.contactimport.presentation.importing.model

import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScannedIdentityPreview
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust

data class ImportIdentityUiState(
    val encodedIdentity: String = "",
    val scannedIdentityPreview: ScannedIdentityPreview? = null,
    val isImporting: Boolean = false,
    val importedContactName: String? = null,
    val importedIdentityTrust: IdentityImportTrust? = null,
    val errorMessage: String? = null
)
