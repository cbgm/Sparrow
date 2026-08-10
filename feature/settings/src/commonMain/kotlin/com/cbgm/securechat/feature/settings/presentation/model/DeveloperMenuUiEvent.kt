package com.cbgm.securechat.feature.settings.presentation.model

sealed interface DeveloperMenuUiEvent {
    data object BackClicked : DeveloperMenuUiEvent

    data object ClearLocalDataClicked : DeveloperMenuUiEvent

    data object DisableDeveloperModeClicked : DeveloperMenuUiEvent
}
