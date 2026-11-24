package com.cbgm.sparrow.feature.onboarding.presentation

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.ui.theme.AppThemeMode
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.onboarding.device.PermissionRequestResult
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiEvent
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiState
import com.cbgm.sparrow.feature.settings.domain.usecase.GetAppThemeModeUseCase
import com.cbgm.sparrow.feature.settings.domain.usecase.SetAppThemeModeUseCase
import com.cbgm.sparrow.feature.transport.background.ConnectionMode
import com.cbgm.sparrow.feature.transport.domain.usecase.CanConnectWhileClosedUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetConnectionModeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val setConnectionMode: SetConnectionModeUseCase,
    canConnectWhileClosed: CanConnectWhileClosedUseCase,
    private val getAppThemeMode: GetAppThemeModeUseCase,
    private val setAppThemeMode: SetAppThemeModeUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState(connectsWhileClosed = canConnectWhileClosed()))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getAppThemeMode().onSuccess { mode -> _uiState.update { state -> state.copy(themeMode = mode) } }
        }
    }

    fun onUiEvent(event: OnboardingUiEvent) {
        when (event) {
            is OnboardingUiEvent.PageShown -> showPage(event.page)
            OnboardingUiEvent.RequestPermissionsClicked -> requestPermissions()
            OnboardingUiEvent.RetryAutomaticNumberClicked -> retryAutomaticPhoneNumber()
            OnboardingUiEvent.ChooseAnotherNumberClicked -> requestPhoneNumberHint()
            is OnboardingUiEvent.ConnectionModeChosen -> chooseConnectionMode(event.mode)
            is OnboardingUiEvent.ThemeModeChosen -> chooseThemeMode(event.mode)
        }
    }

    /** Swiping on past a page does what its action would: asking for the permissions, saving the preselected mode. */
    private fun showPage(page: OnboardingPage) {
        val left = _uiState.value.page
        _uiState.update { state -> state.copy(page = page) }
        if (page <= left) return
        when (left) {
            OnboardingPage.PERMISSIONS -> requestPermissionsOnce()
            OnboardingPage.RECEIVING -> saveConnectionModeOnce()
            OnboardingPage.WELCOME, OnboardingPage.ONION, OnboardingPage.THEME, OnboardingPage.PHONE -> Unit
        }
    }

    private fun requestPermissionsOnce() {
        if (!_uiState.value.permissionsRequested) requestPermissions()
    }

    private fun saveConnectionModeOnce() {
        val state = _uiState.value
        if (state.connectsWhileClosed && !state.connectionModeSaved) chooseConnectionMode(state.connectionMode)
    }

    private fun requestPermissions() {
        _uiState.value =
            _uiState.value.copy(
                permissionRequestId = _uiState.value.permissionRequestId + 1,
                permissionsRequested = true
            )
    }

    fun onPermissionsResult(result: PermissionRequestResult) {
        _uiState.value =
            _uiState.value.copy(
                permissionsRequested = true,
                phonePermissionGranted = result.phoneNumberGranted,
                page = if (_uiState.value.page == OnboardingPage.PERMISSIONS) OnboardingPage.RECEIVING else _uiState.value.page,
                automaticPhoneRequestId =
                    if (result.phoneNumberGranted) {
                        _uiState.value.automaticPhoneRequestId + 1
                    } else {
                        _uiState.value.automaticPhoneRequestId
                    }
            )
    }

    /** Saved at once; "Always connected" also asks Android to leave Sparrow out of battery optimization. */
    private fun chooseConnectionMode(mode: ConnectionMode) {
        _uiState.update { state ->
            state.copy(
                connectionMode = mode,
                connectionModeSaved = true,
                batteryExemptionDeclined = false,
                batteryExemptionRequestId = state.batteryExemptionRequestId + if (mode == ConnectionMode.ALWAYS_CONNECTED) 1 else 0
            )
        }
        viewModelScope.launch {
            setConnectionMode(mode).onFailure { error -> SparrowLog.error("OnboardingViewModel", "Connection mode could not be saved", error) }
        }
    }

    private fun chooseThemeMode(mode: AppThemeMode) {
        _uiState.update { state -> state.copy(themeMode = mode) }
        viewModelScope.launch {
            setAppThemeMode(mode).onFailure { error -> SparrowLog.error("OnboardingViewModel", "Theme mode could not be saved", error) }
        }
    }

    fun onBatteryExemptionResult(exempted: Boolean) {
        _uiState.update { state -> state.copy(batteryExemptionDeclined = !exempted) }
    }

    private fun requestPhoneNumberHint() {
        _uiState.update { state ->
            state.copy(phoneNumberHintRequestId = state.phoneNumberHintRequestId + 1)
        }
    }

    private fun retryAutomaticPhoneNumber() {
        if (!_uiState.value.phonePermissionGranted) return
        _uiState.value =
            _uiState.value.copy(
                automaticPhoneRequestId = _uiState.value.automaticPhoneRequestId + 1
            )
    }
}
