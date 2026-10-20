package com.cbgm.sparrow.feature.identity.presentation.overview.mapper

import com.cbgm.sparrow.feature.identity.domain.model.LocalProfilePicture
import com.cbgm.sparrow.feature.identity.presentation.overview.profile.IdentityProfilePictureUiState

internal fun LocalProfilePicture.toUi(isSaving: Boolean): IdentityProfilePictureUiState =
    IdentityProfilePictureUiState(hasPicture = hasPicture, isSaving = isSaving)
