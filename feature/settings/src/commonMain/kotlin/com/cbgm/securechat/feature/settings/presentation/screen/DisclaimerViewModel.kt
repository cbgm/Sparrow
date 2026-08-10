package com.cbgm.securechat.feature.settings.presentation.screen

import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.settings.presentation.model.DisclaimerUiEvent

class DisclaimerViewModel : BaseViewModel() {
    fun onUiEvent(event: DisclaimerUiEvent) {
        when (event) {
            DisclaimerUiEvent.BackClicked -> navigator.popBackStack()
        }
    }
}
