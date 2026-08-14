package com.cbgm.sparrow.feature.contactimport.presentation.importing.model

import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust

data class ImportIdentityUiState(
    val encodedIdentity: String = "",
    val isImporting: Boolean = false,
    val importedContactName: String? = null,
    val importedIdentityTrust: IdentityImportTrust? = null,
    val errorMessage: String? = null
)
