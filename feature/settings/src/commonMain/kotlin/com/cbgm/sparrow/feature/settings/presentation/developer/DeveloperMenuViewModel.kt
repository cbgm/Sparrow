package com.cbgm.sparrow.feature.settings.presentation.developer

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.transport.TransportDiagnosticsProvider
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.settings.domain.usecase.ClearLocalDataUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.GetBuildInfoUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.SetDeveloperEnabledUseCase
import com.cbgm.sparrow.feature.settings.presentation.developer.model.DeveloperMenuUiEvent
import com.cbgm.sparrow.feature.settings.presentation.developer.model.DeveloperMenuUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class DeveloperMenuViewModel(
    private val clearLocalDataUseCase: ClearLocalDataUseCase,
    private val getBuildInfoUseCase: GetBuildInfoUseCase,
    private val setDeveloperEnabledUseCase: SetDeveloperEnabledUseCase,
    private val transportDiagnosticsProvider: TransportDiagnosticsProvider
) : BaseViewModel() {
    private val _uiState =
        MutableStateFlow(
            DeveloperMenuUiState(
                buildInfo = getBuildInfoUseCase(),
                transportDiagnostics = transportDiagnosticsProvider.diagnostics.value
            )
        )
    val uiState: StateFlow<DeveloperMenuUiState> = _uiState.asStateFlow()

    init {
        observeTransportDiagnostics()
        refreshTransportDiagnostics()
    }

    fun onUiEvent(event: DeveloperMenuUiEvent) {
        when (event) {
            DeveloperMenuUiEvent.BackClicked -> navigator.popBackStack()
            DeveloperMenuUiEvent.ClearLocalDataClicked -> onClearLocalData()
            DeveloperMenuUiEvent.DisableDeveloperModeClicked -> onDisableDeveloperMode()
        }
    }

    private fun onClearLocalData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingLocalData = true) }
            clearLocalDataUseCase()
            _uiState.update { it.copy(isClearingLocalData = false) }
        }
    }

    private fun onDisableDeveloperMode() {
        viewModelScope.launch {
            setDeveloperEnabledUseCase(false)
            navigator.popBackStack()
        }
    }

    private fun observeTransportDiagnostics() {
        viewModelScope.launch {
            transportDiagnosticsProvider.diagnostics.collectLatest { diagnostics ->
                _uiState.update { current ->
                    current.copy(transportDiagnostics = diagnostics)
                }
            }
        }
    }

    private fun refreshTransportDiagnostics() {
        viewModelScope.launch {
            while (isActive) {
                transportDiagnosticsProvider.refreshDiagnostics()
                delay(DIAGNOSTICS_REFRESH_INTERVAL_MILLISECONDS.milliseconds)
            }
        }
    }

    private companion object {
        const val DIAGNOSTICS_REFRESH_INTERVAL_MILLISECONDS = 1_000L
    }
}
