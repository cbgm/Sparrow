package com.cbgm.sparrow.startup.presentation.screen

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.startup.AppInitializer
import com.cbgm.sparrow.startup.presentation.model.StartupUiEvent
import com.cbgm.sparrow.startup.presentation.model.StartupUiState
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

    fun onUiEvent(event: StartupUiEvent) {
        when (event) {
            StartupUiEvent.RetryClicked -> retry()
            StartupUiEvent.RequestPhoneNumberHint,
            is StartupUiEvent.PhoneNumberChanged,
            StartupUiEvent.CreateIdentityClicked -> Unit
        }
    }

    init {
        initialize()
    }

    fun completeStartup() {
        navigator.navigateTo(
            route = AppRoute.Main,
            popUpTo = AppRoute.Startup,
            inclusive = true
        )
    }

    private fun retry() {
        if (mutableUiState.value !is StartupUiState.Error) return

        initializationCompleted = false
        initialize()
    }

    private fun initialize() {
        if (initializationCompleted) {
            return
        }

        viewModelScope.launch {
            mutableUiState.value = StartupUiState.Loading

            appInitializer
                .initialize()
                .onSuccess { result ->
                    initializationCompleted = true

                    mutableUiState.value =
                        if (result.identityReady) {
                            StartupUiState.Ready
                        } else {
                            StartupUiState.IdentityRequired
                        }
                }.onFailure { error ->
                    mutableUiState.value =
                        StartupUiState.Error(
                            message = error.message ?: "Sparrow could not complete startup."
                        )
                }
        }
    }
}
