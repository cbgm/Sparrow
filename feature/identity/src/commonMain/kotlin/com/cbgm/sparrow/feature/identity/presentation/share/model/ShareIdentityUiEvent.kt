package com.cbgm.sparrow.feature.identity.presentation.share.model

sealed interface ShareIdentityUiEvent {
    data object GenerateClicked : ShareIdentityUiEvent

    data object BackClicked : ShareIdentityUiEvent

    data object ShareClicked : ShareIdentityUiEvent
}
