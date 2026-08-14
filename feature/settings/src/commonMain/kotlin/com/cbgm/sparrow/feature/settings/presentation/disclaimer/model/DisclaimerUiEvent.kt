package com.cbgm.sparrow.feature.settings.presentation.disclaimer.model

sealed interface DisclaimerUiEvent {
    data object BackClicked : DisclaimerUiEvent
}
