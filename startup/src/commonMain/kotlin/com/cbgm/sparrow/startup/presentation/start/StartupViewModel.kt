package com.cbgm.sparrow.startup.presentation.start

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.StartupTrace
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.startup.domain.model.StartupResult
import com.cbgm.sparrow.startup.domain.runner.StartupRunner
import com.cbgm.sparrow.startup.presentation.start.model.StartupUiEvent
import com.cbgm.sparrow.startup.presentation.start.model.StartupUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StartupViewModel(
    private val startupRunner: StartupRunner
) : BaseViewModel() {
    private val mutableUiState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)
    private var startupJob: Job? = null
    private var startupCompleted = false

    val uiState: StateFlow<StartupUiState> = mutableUiState.asStateFlow()

    init {
        StartupTrace.event("StartupViewModel created")
        initialize()
    }

    fun onUiEvent(event: StartupUiEvent) {
        when (event) {
            StartupUiEvent.IdentityCreated -> publishReadyState()
            StartupUiEvent.RetryClicked -> initialize()
        }
    }

    fun completeStartup() {
        if (startupCompleted) return
        startupCompleted = true

        StartupTrace.event("StartupViewModel navigation to Main requested")
        navigator.navigateTo(
            route = AppRoute.Main,
            popUpTo = AppRoute.Startup,
            inclusive = true
        )

        startupRunner.startPostNavigationRuntime()
    }

    private fun initialize() {
        startupJob?.cancel()
        startupJob =
            viewModelScope.launch {
                mutableUiState.value = StartupUiState.Loading
                StartupTrace.event("startup UI Loading emitted")

                when (val result = startupRunner.run()) {
                    StartupResult.Ready -> publishReadyState()
                    StartupResult.IdentityRequired -> {
                        mutableUiState.value = StartupUiState.IdentityRequired
                        StartupTrace.event("startup UI state published=IdentityRequired")
                    }
                    is StartupResult.Error -> {
                        mutableUiState.value =
                            StartupUiState.Error(
                                result.cause.message
                                    ?: "Sparrow could not complete startup."
                            )
                        StartupTrace.event("startup UI state published=Error")
                    }
                }
            }
    }

    private fun publishReadyState() {
        mutableUiState.value = StartupUiState.Ready
        StartupTrace.event("startup UI state published=Ready")
    }
}
