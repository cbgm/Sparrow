package com.cbgm.sparrow.feature.identity.presentation.details.model

data class IdentityDetailsUiState(
    val isLoading: Boolean = false,
    val error: String = "",
    val publicIdentity: PublicHexUi? = null
)

data class PublicHexUi(
    val encryptionPublicKey: String,
    val signingPublicKey: String
)
