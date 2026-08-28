package com.cbgm.sparrow.feature.settings.presentation.errors.model

sealed interface DeveloperErrorLogUiEvent {
    data object BackClicked : DeveloperErrorLogUiEvent

    data object ClearErrorsClicked : DeveloperErrorLogUiEvent

    data object ClearErrorsConfirmed : DeveloperErrorLogUiEvent

    data object ClearErrorsDismissed : DeveloperErrorLogUiEvent
}
