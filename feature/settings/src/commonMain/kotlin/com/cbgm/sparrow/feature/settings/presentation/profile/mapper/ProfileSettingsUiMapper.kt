package com.cbgm.sparrow.feature.settings.presentation.profile.mapper

import com.cbgm.sparrow.feature.identity.domain.model.LocalProfilePicture
import com.cbgm.sparrow.feature.settings.presentation.profile.model.ProfileSettingsUiState

internal fun LocalProfilePicture.toProfileSettingsUiState(
    isSaving: Boolean,
    errorMessage: String?
): ProfileSettingsUiState =
    ProfileSettingsUiState(
        profilePicture = this,
        isSaving = isSaving,
        errorMessage = errorMessage
    )
