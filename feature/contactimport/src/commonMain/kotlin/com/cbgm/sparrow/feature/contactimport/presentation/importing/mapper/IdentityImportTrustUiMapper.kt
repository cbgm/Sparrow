package com.cbgm.sparrow.feature.contactimport.presentation.importing.mapper

import com.cbgm.sparrow.feature.contactimport.presentation.importing.model.IdentityImportTrustUi
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityImportTrust

internal fun IdentityImportTrust.toUi(): IdentityImportTrustUi = when (this) {
    IdentityImportTrust.UNVERIFIED -> IdentityImportTrustUi.UNVERIFIED
    IdentityImportTrust.VERIFIED_IN_PERSON -> IdentityImportTrustUi.VERIFIED_IN_PERSON
}
