package com.cbgm.sparrow.feature.identity.presentation.overview.model

sealed interface IdentityOverviewUiEvent {
    data object RetryClicked : IdentityOverviewUiEvent

    data object ShareIdentityClicked : IdentityOverviewUiEvent

    data object OpenKeysClicked : IdentityOverviewUiEvent

    data object OpenBackupClicked : IdentityOverviewUiEvent
}
