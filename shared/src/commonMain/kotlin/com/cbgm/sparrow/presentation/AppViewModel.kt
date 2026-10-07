package com.cbgm.sparrow.presentation

import androidx.lifecycle.ViewModel
import com.cbgm.sparrow.core.logging.StartupTrace
import com.cbgm.sparrow.runtime.foreground.ForegroundRuntimeCoordinator

class AppViewModel(
    private val foregroundRuntime: ForegroundRuntimeCoordinator
) : ViewModel() {
    init {
        StartupTrace.begin()
    }

    fun onAppVisible() {
        foregroundRuntime.onAppVisible()
    }

    fun onAppHidden() {
        foregroundRuntime.onAppHidden()
    }
}
