package com.cbgm.sparrow.startup.presentation.start.model

sealed interface StartupUiEvent {
    data object IdentityCreated : StartupUiEvent

    data object RetryClicked : StartupUiEvent
}
