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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class DeveloperMenuViewModel(
    private val clearLocalDataUseCase: ClearLocalDataUseCase,
    getBuildInfoUseCase: GetBuildInfoUseCase,
    private val setDeveloperEnabledUseCase: SetDeveloperEnabledUseCase,
    private val transportDiagnosticsProvider: TransportDiagnosticsProvider
) : BaseViewModel() {
    private val isClearingLocalData = MutableStateFlow(false)
    private val buildInfo = getBuildInfoUseCase()

    val uiState: StateFlow<DeveloperMenuUiState> =
        combine(
            transportDiagnosticsProvider.diagnostics,
            isClearingLocalData
        ) { diagnostics, isClearing ->
            DeveloperMenuUiState(
                buildInfo = buildInfo,
                transportDiagnostics = diagnostics,
                isClearingLocalData = isClearing
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue =
                DeveloperMenuUiState(
                    buildInfo = buildInfo,
                    transportDiagnostics = transportDiagnosticsProvider.diagnostics.value
                )
        )

    init {
        refreshTransportDiagnostics()
    }

    fun onUiEvent(event: DeveloperMenuUiEvent) {
        when (event) {
            DeveloperMenuUiEvent.BackClicked -> navigator.popBackStack()
            DeveloperMenuUiEvent.ClearLocalDataClicked -> clearLocalData()
            DeveloperMenuUiEvent.DisableDeveloperModeClicked -> disableDeveloperMode()
        }
    }

    private fun clearLocalData() {
        if (isClearingLocalData.value) return

        viewModelScope.launch {
            isClearingLocalData.value = true
            try {
                clearLocalDataUseCase()
            } finally {
                isClearingLocalData.value = false
            }
        }
    }

    private fun disableDeveloperMode() {
        viewModelScope.launch {
            setDeveloperEnabledUseCase(false)
            navigator.popBackStack()
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
