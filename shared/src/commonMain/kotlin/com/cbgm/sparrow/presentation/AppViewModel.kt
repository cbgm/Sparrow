package com.cbgm.sparrow.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.logging.StartupTrace
import com.cbgm.sparrow.feature.applock.domain.usecase.ObserveAppLockEnabledUseCase
import com.cbgm.sparrow.feature.applock.domain.usecase.RecordAppHiddenUseCase
import com.cbgm.sparrow.feature.applock.domain.usecase.RecordAppVisibleUseCase
import com.cbgm.sparrow.runtime.foreground.ForegroundRuntimeCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AppViewModel(
    private val foregroundRuntime: ForegroundRuntimeCoordinator,
    private val recordAppHidden: RecordAppHiddenUseCase,
    private val recordAppVisible: RecordAppVisibleUseCase,
    observeAppLockEnabled: ObserveAppLockEnabledUseCase
) : ViewModel() {
    private val logger = SparrowLog.withTag("AppViewModel")

    /** While the app lock is on, the app's content must not show up in the recent-apps preview. */
    val appLockEnabled: Flow<Boolean> = observeAppLockEnabled()

    init {
        StartupTrace.begin()
    }

    fun onAppVisible() {
        recordAppVisible()
        foregroundRuntime.onAppVisible()
    }

    fun onAppHidden() {
        viewModelScope.launch {
            recordAppHidden().onFailure { error -> logger.warn(error) { "App lock timeout could not be started" } }
        }
        foregroundRuntime.onAppHidden()
    }
}
