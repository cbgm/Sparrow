package com.cbgm.securechat.feature.settings.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.transport.TransportDiagnosticsProvider
import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository
import com.cbgm.securechat.feature.settings.presentation.model.DeveloperMenuUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeveloperMenuViewModel(
    private val settingsRepository: SettingsRepository,
    transportDiagnosticsProvider: TransportDiagnosticsProvider
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            DeveloperMenuUiState(
                buildInfo = settingsRepository.getBuildInfo(),
                transportDiagnostics = transportDiagnosticsProvider.diagnostics.value
            )
        )
    val uiState: StateFlow<DeveloperMenuUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            transportDiagnosticsProvider.diagnostics.collectLatest { diagnostics ->
                _uiState.update { current ->
                    current.copy(transportDiagnostics = diagnostics)
                }
            }
        }
    }

    fun onClearLocalData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingLocalData = true) }
            settingsRepository.clearLocalData()
            _uiState.update { it.copy(isClearingLocalData = false) }
        }
    }

    fun onDisableDeveloperMode() {
        viewModelScope.launch {
            settingsRepository.setDeveloperModeEnabled(false)
        }
    }
}
