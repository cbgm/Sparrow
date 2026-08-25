package com.cbgm.sparrow.feature.safety.presentation.details.model

sealed interface MessageSafetyDetailsUiEvent {
    data object BackClicked : MessageSafetyDetailsUiEvent

    data object BlockUserClicked : MessageSafetyDetailsUiEvent
}
