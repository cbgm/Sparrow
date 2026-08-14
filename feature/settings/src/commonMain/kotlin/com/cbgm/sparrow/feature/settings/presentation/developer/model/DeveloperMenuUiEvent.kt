package com.cbgm.sparrow.feature.settings.presentation.developer.model

sealed interface DeveloperMenuUiEvent {
    data object BackClicked : DeveloperMenuUiEvent

    data object ClearLocalDataClicked : DeveloperMenuUiEvent

    data object DisableDeveloperModeClicked : DeveloperMenuUiEvent
}
