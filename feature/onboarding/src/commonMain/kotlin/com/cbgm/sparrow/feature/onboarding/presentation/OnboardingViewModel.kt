package com.cbgm.sparrow.feature.onboarding.presentation

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.onboarding.device.PermissionRequestResult
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiEvent
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiState
import com.cbgm.sparrow.feature.transport.background.ConnectionMode
import com.cbgm.sparrow.feature.transport.domain.usecase.CanConnectWhileClosedUseCase
import com.cbgm.sparrow.feature.transport.domain.usecase.SetConnectionModeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val setConnectionMode: SetConnectionModeUseCase,
    canConnectWhileClosed: CanConnectWhileClosedUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState(connectsWhileClosed = canConnectWhileClosed()))
    val uiState = _uiState.asStateFlow()

    fun onUiEvent(event: OnboardingUiEvent) {
        when (event) {
            OnboardingUiEvent.NextClicked -> next()
            OnboardingUiEvent.RequestPermissionsClicked -> requestPermissions()
            OnboardingUiEvent.RetryAutomaticNumberClicked -> retryAutomaticPhoneNumber()
            OnboardingUiEvent.ChooseAnotherNumberClicked -> requestPhoneNumberHint()
            is OnboardingUiEvent.ConnectionModeChosen -> chooseConnectionMode(event.mode)
        }
    }

    private fun next() {
        _uiState.value =
            when (_uiState.value.page) {
                OnboardingPage.WELCOME -> _uiState.value.copy(page = OnboardingPage.PRIVACY)
                OnboardingPage.PRIVACY -> _uiState.value.copy(page = OnboardingPage.PERMISSIONS)
                OnboardingPage.PERMISSIONS -> _uiState.value.copy(page = OnboardingPage.RECEIVING)
                OnboardingPage.RECEIVING -> _uiState.value.copy(page = OnboardingPage.PHONE)
                OnboardingPage.PHONE -> _uiState.value
            }
    }

    private fun requestPermissions() {
        _uiState.value =
            _uiState.value.copy(
                permissionRequestId = _uiState.value.permissionRequestId + 1
            )
    }

    fun onPermissionsResult(result: PermissionRequestResult) {
        _uiState.value =
            _uiState.value.copy(
                permissionsRequested = true,
                phonePermissionGranted = result.phoneNumberGranted,
                page = OnboardingPage.RECEIVING,
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
                batteryExemptionDeclined = false,
                batteryExemptionRequestId = state.batteryExemptionRequestId + if (mode == ConnectionMode.ALWAYS_CONNECTED) 1 else 0
            )
        }
        viewModelScope.launch {
            setConnectionMode(mode).onFailure { error -> SparrowLog.error("OnboardingViewModel", "Connection mode could not be saved", error) }
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
