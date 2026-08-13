package com.cbgm.securechat.feature.identity.presentation.setup.model

import com.cbgm.securechat.feature.identity.domain.model.PublicIdentity

sealed interface IdentityUiState {
    data object Loading : IdentityUiState

    data class NoIdentity(
        val phoneNumber: String = "",
        val name: String = "",
        val phoneNumberError: String? = null
    ) : IdentityUiState

    data class Ready(
        val publicIdentity: PublicIdentity,
        val localPhoneNumber: String
    ) : IdentityUiState

    data object IncompleteIdentity : IdentityUiState

    data class Error(
        val message: String
    ) : IdentityUiState
}
