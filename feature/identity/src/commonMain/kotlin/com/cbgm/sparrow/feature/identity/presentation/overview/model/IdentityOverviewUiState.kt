package com.cbgm.sparrow.feature.identity.presentation.overview.model

import com.cbgm.sparrow.feature.identity.presentation.backup.model.BackupStateUi
import com.cbgm.sparrow.feature.identity.presentation.overview.profile.IdentityProfilePictureUiState

sealed interface IdentityOverviewUiState {
    data object Loading : IdentityOverviewUiState

    data class Ready(
        val localPhoneNumber: String,
        val backupStatus: BackupStateUi = BackupStateUi.NOT_BACKED_UP,
        val profilePicture: IdentityProfilePictureUiState = IdentityProfilePictureUiState()
    ) : IdentityOverviewUiState

    data object Error : IdentityOverviewUiState
}
