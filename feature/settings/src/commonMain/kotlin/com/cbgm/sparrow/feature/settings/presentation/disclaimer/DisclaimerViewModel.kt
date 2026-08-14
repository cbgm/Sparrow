package com.cbgm.sparrow.feature.settings.presentation.disclaimer

import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.settings.presentation.disclaimer.model.DisclaimerUiEvent

class DisclaimerViewModel : BaseViewModel() {
    fun onUiEvent(event: DisclaimerUiEvent) {
        when (event) {
            DisclaimerUiEvent.BackClicked -> navigator.popBackStack()
        }
    }
}
