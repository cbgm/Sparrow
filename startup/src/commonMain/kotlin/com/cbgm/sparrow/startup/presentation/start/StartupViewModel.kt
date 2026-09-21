package com.cbgm.sparrow.startup.presentation.start

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.logging.StartupTrace
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.startup.presentation.start.model.AppInitializationResult
import com.cbgm.sparrow.startup.presentation.start.model.StartupConnection
import com.cbgm.sparrow.startup.presentation.start.model.StartupUiEvent
import com.cbgm.sparrow.startup.presentation.start.model.StartupUiState
import com.cbgm.sparrow.startup.util.AppInitializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StartupViewModel(
    private val appInitializer: AppInitializer
) : BaseViewModel() {
    private val mutableUiState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)

    val uiState: StateFlow<StartupUiState> = mutableUiState.asStateFlow()

    private var initializationCompleted = false

    init {
        StartupTrace.event("StartupViewModel created")
        initialize()
    }

    fun onUiEvent(event: StartupUiEvent) {
        when (event) {
            StartupUiEvent.IdentityCreated -> restartInitialization()
            StartupUiEvent.RetryClicked -> retry()
            StartupUiEvent.RequestPhoneNumberHint,
            is StartupUiEvent.PhoneNumberChanged,
            StartupUiEvent.CreateIdentityClicked -> Unit
        }
    }

    fun completeStartup() {
        StartupTrace.event("StartupViewModel navigation to Main requested")
        navigator.navigateTo(
            route = AppRoute.Main,
            popUpTo = AppRoute.Startup,
            inclusive = true
        )
    }

    private fun retry() {
        if (mutableUiState.value !is StartupUiState.Error) return
        restartInitialization()
    }

    private fun restartInitialization() {
        initializationCompleted = false
        initialize()
    }

    private fun initialize() {
        if (initializationCompleted) return

        viewModelScope.launch {
            mutableUiState.value = StartupUiState.Loading
            StartupTrace.event("startup UI Loading emitted")

            appInitializer
                .initialize()
                .onSuccess { result ->
                    StartupTrace.event("startup initializer result=$result")
                    initializationCompleted = result !is AppInitializationResult.IdentityRequired
                    mutableUiState.value = result.toStartupUiState()
                    StartupTrace.event("startup UI state published=$result")
                }.onFailure { error ->
                    StartupTrace.event("startup initializer failure: ${error.message}")
                    SparrowLog.error("StartupViewModel", "Sparrow could not complete startup", error)
                    mutableUiState.value =
                        StartupUiState.Error(
                            message = error.message ?: "Sparrow could not complete startup."
                        )
                }
        }
    }
}

private fun AppInitializationResult.toStartupUiState(): StartupUiState =
    when (this) {
        AppInitializationResult.IdentityRequired -> StartupUiState.IdentityRequired
        AppInitializationResult.ReadyOnline -> StartupUiState.Ready(StartupConnection.ONLINE)
        AppInitializationResult.ReadyOffline -> StartupUiState.Ready(StartupConnection.OFFLINE)
    }
