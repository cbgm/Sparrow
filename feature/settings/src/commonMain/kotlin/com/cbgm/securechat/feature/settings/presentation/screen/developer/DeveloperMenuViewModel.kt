package com.cbgm.securechat.feature.settings.presentation.screen.developer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.transport.TransportDiagnosticsProvider
import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository
import com.cbgm.securechat.feature.settings.presentation.model.DeveloperMenuUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DeveloperMenuViewModel(
    private val settingsRepository: SettingsRepository,
    private val transportDiagnosticsProvider: TransportDiagnosticsProvider
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
        observeTransportDiagnostics()
        refreshTransportDiagnostics()
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
                delay(DIAGNOSTICS_REFRESH_INTERVAL_MILLISECONDS)
            }
        }
    }

    private companion object {
        const val DIAGNOSTICS_REFRESH_INTERVAL_MILLISECONDS = 1_000L
    }
}
