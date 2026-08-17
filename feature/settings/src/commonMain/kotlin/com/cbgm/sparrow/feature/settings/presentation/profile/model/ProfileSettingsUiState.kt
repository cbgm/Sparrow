package com.cbgm.sparrow.feature.settings.presentation.profile.model

import com.cbgm.sparrow.feature.identity.domain.model.LocalProfilePicture

data class ProfileSettingsUiState(
    val profilePicture: LocalProfilePicture = LocalProfilePicture(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
