package com.cbgm.sparrow.feature.identity.presentation.create.model

sealed interface IdentityCreateUiState {
    data object Loading : IdentityCreateUiState

    data object Creating : IdentityCreateUiState

    data class Form(
        val phoneNumber: String = "",
        val name: String = ""
    ) : IdentityCreateUiState

    data object Completed : IdentityCreateUiState

    data object IncompleteIdentity : IdentityCreateUiState

    data object Error : IdentityCreateUiState
}
