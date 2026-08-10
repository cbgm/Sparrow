package com.cbgm.securechat.feature.identity.presentation.model

sealed interface ShareIdentityUiEvent {
    data object GenerateClicked : ShareIdentityUiEvent

    data object BackClicked : ShareIdentityUiEvent

    data object ShareClicked : ShareIdentityUiEvent
}
