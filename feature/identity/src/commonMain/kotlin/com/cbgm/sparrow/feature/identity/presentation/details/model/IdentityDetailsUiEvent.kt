package com.cbgm.sparrow.feature.identity.presentation.details.model

sealed interface IdentityDetailsUiEvent {
    data object BackClicked : IdentityDetailsUiEvent

    data object ShareIdentityClicked : IdentityDetailsUiEvent

    data object RetryClicked : IdentityDetailsUiEvent
}
