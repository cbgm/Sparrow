package com.cbgm.sparrow.feature.onboarding.presentation

import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.onboarding.device.PermissionRequestResult
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiEvent
import com.cbgm.sparrow.feature.onboarding.presentation.model.OnboardingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingViewModel : BaseViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onUiEvent(event: OnboardingUiEvent) {
        when (event) {
            OnboardingUiEvent.NextClicked -> next()
            OnboardingUiEvent.RequestPermissionsClicked -> requestPermissions()
            OnboardingUiEvent.RetryAutomaticNumberClicked -> retryAutomaticPhoneNumber()
            OnboardingUiEvent.ChooseAnotherNumberClicked,
            is OnboardingUiEvent.PhoneNumberChanged,
            is OnboardingUiEvent.NameChanged,
            OnboardingUiEvent.ApproveAndCreateClicked -> Unit
        }
    }

    private fun next() {
        _uiState.value =
            when (_uiState.value.page) {
                OnboardingPage.WELCOME -> _uiState.value.copy(page = OnboardingPage.PRIVACY)
                OnboardingPage.PRIVACY -> _uiState.value.copy(page = OnboardingPage.PERMISSIONS)
                OnboardingPage.PERMISSIONS -> _uiState.value.copy(page = OnboardingPage.PHONE)
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
                page = OnboardingPage.PHONE,
                automaticPhoneRequestId =
                    if (result.phoneNumberGranted) {
                        _uiState.value.automaticPhoneRequestId + 1
                    } else {
                        _uiState.value.automaticPhoneRequestId
                    }
            )
    }

    private fun retryAutomaticPhoneNumber() {
        if (!_uiState.value.phonePermissionGranted) return
        _uiState.value =
            _uiState.value.copy(
                automaticPhoneRequestId = _uiState.value.automaticPhoneRequestId + 1
            )
    }

    fun setCreatingIdentity(value: Boolean) {
        _uiState.value = _uiState.value.copy(isCreatingIdentity = value)
    }
}
