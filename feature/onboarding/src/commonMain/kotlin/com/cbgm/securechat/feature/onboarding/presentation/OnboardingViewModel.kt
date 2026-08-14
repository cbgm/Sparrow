package com.cbgm.securechat.feature.onboarding.presentation

import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.onboarding.device.PermissionRequestResult
import com.cbgm.securechat.feature.onboarding.presentation.model.OnboardingPage
import com.cbgm.securechat.feature.onboarding.presentation.model.OnboardingUiEvent
import com.cbgm.securechat.feature.onboarding.presentation.model.OnboardingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingViewModel : BaseViewModel() {
    private val mutableState = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = mutableState.asStateFlow()

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
        mutableState.value =
            when (mutableState.value.page) {
                OnboardingPage.WELCOME -> mutableState.value.copy(page = OnboardingPage.PRIVACY)
                OnboardingPage.PRIVACY -> mutableState.value.copy(page = OnboardingPage.PERMISSIONS)
                OnboardingPage.PERMISSIONS -> mutableState.value.copy(page = OnboardingPage.PHONE)
                OnboardingPage.PHONE -> mutableState.value
            }
    }

    private fun requestPermissions() {
        mutableState.value =
            mutableState.value.copy(
                permissionRequestId = mutableState.value.permissionRequestId + 1
            )
    }

    fun onPermissionsResult(result: PermissionRequestResult) {
        mutableState.value =
            mutableState.value.copy(
                permissionsRequested = true,
                phonePermissionGranted = result.phoneNumberGranted,
                page = OnboardingPage.PHONE,
                automaticPhoneRequestId =
                    if (result.phoneNumberGranted) {
                        mutableState.value.automaticPhoneRequestId + 1
                    } else {
                        mutableState.value.automaticPhoneRequestId
                    }
            )
    }

    private fun retryAutomaticPhoneNumber() {
        if (!mutableState.value.phonePermissionGranted) return
        mutableState.value =
            mutableState.value.copy(
                automaticPhoneRequestId = mutableState.value.automaticPhoneRequestId + 1
            )
    }

    fun setCreatingIdentity(value: Boolean) {
        mutableState.value = mutableState.value.copy(isCreatingIdentity = value)
    }
}
