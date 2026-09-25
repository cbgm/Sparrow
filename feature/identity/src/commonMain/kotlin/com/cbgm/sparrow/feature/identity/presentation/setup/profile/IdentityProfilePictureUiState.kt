package com.cbgm.sparrow.feature.identity.presentation.setup.profile

data class IdentityProfilePictureUiState(
    val hasPicture: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
