package com.cbgm.sparrow.feature.polls.presentation.message

import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PollMessageViewModel : BaseViewModel() {
    private val _isVotersOverlayVisible = MutableStateFlow(false)
    val isVotersOverlayVisible: StateFlow<Boolean> = _isVotersOverlayVisible.asStateFlow()

    fun openVoters() {
        _isVotersOverlayVisible.value = true
    }

    fun dismissVoters() {
        _isVotersOverlayVisible.value = false
    }
}
