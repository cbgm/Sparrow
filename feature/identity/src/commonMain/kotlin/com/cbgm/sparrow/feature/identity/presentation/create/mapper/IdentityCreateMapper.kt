package com.cbgm.sparrow.feature.identity.presentation.create.mapper

import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.presentation.create.model.IdentityCreateUiState

internal fun IdentityStatus.toUi(phoneNumber: String, name: String): IdentityCreateUiState = when (this) {
    IdentityStatus.NOT_CREATED -> IdentityCreateUiState.Form(phoneNumber = phoneNumber, name = name)
    IdentityStatus.INCOMPLETE -> IdentityCreateUiState.IncompleteIdentity
    IdentityStatus.READY -> IdentityCreateUiState.Completed
}
