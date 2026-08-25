package com.cbgm.sparrow.notification.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppVisibilityState {
    private val mutableIsVisible = MutableStateFlow(false)

    val isVisible: StateFlow<Boolean> = mutableIsVisible.asStateFlow()

    fun onAppVisible() {
        mutableIsVisible.value = true
    }

    fun onAppHidden() {
        mutableIsVisible.value = false
    }
}
