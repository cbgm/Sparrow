package com.cbgm.securechat.feature.settings.presentation.disclaimer

import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.settings.presentation.disclaimer.model.DisclaimerUiEvent

class DisclaimerViewModel : BaseViewModel() {
    fun onUiEvent(event: DisclaimerUiEvent) {
        when (event) {
            DisclaimerUiEvent.BackClicked -> navigator.popBackStack()
        }
    }
}
