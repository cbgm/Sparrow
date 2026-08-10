package com.cbgm.securechat.feature.settings.presentation.model

sealed interface DisclaimerUiEvent {
    data object BackClicked : DisclaimerUiEvent
}
