package com.cbgm.securechat.feature.settings.presentation.disclaimer.model

sealed interface DisclaimerUiEvent {
    data object BackClicked : DisclaimerUiEvent
}
