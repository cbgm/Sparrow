package com.cbgm.sparrow.feature.identity.presentation.verification

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.identity.device.PhoneVerificationRequest
import com.cbgm.sparrow.feature.identity.device.PhoneVerificationResult
import com.cbgm.sparrow.feature.identity.domain.usecase.GetPhoneVerificationPolicyUseCase
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationStage
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_identity_phone_verification_policy_failed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class PhoneVerificationViewModel(
    private val getPolicy: GetPhoneVerificationPolicyUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(PhoneVerificationUiState())
    val uiState = _uiState.asStateFlow()
    private var nextRequestId = 0
    private var activeSendRequestId = 0
    private var activeConfirmRequestId = 0
    private var policyGeneration = 0

    fun start(phoneNumber: String) {
        if (phoneNumber.isBlank() || _uiState.value.phoneNumber == phoneNumber &&
            _uiState.value.stage != PhoneVerificationStage.IDLE
        ) {
            return
        }
        val generation = ++policyGeneration
        _uiState.value = PhoneVerificationUiState(
            phoneNumber = phoneNumber,
            stage = PhoneVerificationStage.LOADING_POLICY
        )
        viewModelScope.launch {
            getPolicy().fold(
                onSuccess = { required ->
                    if (policyGeneration != generation || _uiState.value.phoneNumber != phoneNumber) return@fold
                    _uiState.value = _uiState.value.copy(
                        required = required,
                        stage = PhoneVerificationStage.READY,
                        hasError = false
                    )
                },
                onFailure = { error ->
                    SparrowLog.error("PhoneVerification", "Could not load verification policy", error)
                    if (policyGeneration == generation && _uiState.value.phoneNumber == phoneNumber) {
                        _uiState.value = _uiState.value.copy(
                            stage = PhoneVerificationStage.IDLE,
                            hasError = true
                        )
                        SparrowLog.showError(getString(Res.string.feature_identity_phone_verification_policy_failed))
                    }
                }
            )
        }
    }

    fun retry() {
        val phone = _uiState.value.phoneNumber
        _uiState.value = PhoneVerificationUiState()
        start(phone)
    }

    fun sendCode() {
        val state = _uiState.value
        if (state.stage != PhoneVerificationStage.READY) return
        activeSendRequestId = nextRequestId + 1
        activeConfirmRequestId = 0
        _uiState.value = state.copy(
            stage = PhoneVerificationStage.SENDING_CODE,
            hasError = false,
            verificationId = "",
            code = "",
            request = PhoneVerificationRequest.Send(++nextRequestId, state.phoneNumber)
        )
    }

    fun codeChanged(value: String) {
        val state = _uiState.value
        if (state.stage == PhoneVerificationStage.ENTER_CODE) {
            _uiState.value = state.copy(code = value.filter(Char::isDigit).take(6), hasError = false)
        }
    }

    fun confirm() {
        val state = _uiState.value
        if (!state.canConfirm) return
        activeConfirmRequestId = nextRequestId + 1
        _uiState.value = state.copy(
            stage = PhoneVerificationStage.CHECKING_CODE,
            hasError = false,
            request = PhoneVerificationRequest.Confirm(++nextRequestId, state.phoneNumber, state.verificationId, state.code)
        )
    }

    fun onResult(result: PhoneVerificationResult) {
        val state = _uiState.value
        if (state.stage == PhoneVerificationStage.IDLE || state.stage == PhoneVerificationStage.VERIFIED ||
            state.stage == PhoneVerificationStage.LOADING_POLICY
        ) {
            return
        }
        if (result.requestId != activeSendRequestId && result.requestId != activeConfirmRequestId) return
        when (result) {
            is PhoneVerificationResult.CodeSent -> {
                if (state.stage == PhoneVerificationStage.SENDING_CODE) {
                    _uiState.value = state.copy(
                        stage = PhoneVerificationStage.ENTER_CODE,
                        verificationId = result.verificationId,
                        request = null
                    )
                }
            }
            is PhoneVerificationResult.Verified -> _uiState.value = state.copy(
                stage = PhoneVerificationStage.VERIFIED,
                request = null
            )
            is PhoneVerificationResult.Unsupported -> _uiState.value = state.copy(
                stage = PhoneVerificationStage.READY,
                hasError = false,
                unsupported = true,
                request = null
            )
            is PhoneVerificationResult.Failed -> _uiState.value = state.copy(
                stage = if (state.verificationId.isNotBlank()) {
                    PhoneVerificationStage.ENTER_CODE
                } else {
                    PhoneVerificationStage.SENDING_CODE
                },
                hasError = true,
                request = null
            )
        }
    }

    fun skip() {
        if (_uiState.value.canSkip) {
            _uiState.value = _uiState.value.copy(stage = PhoneVerificationStage.VERIFIED, request = null)
        }
    }

    fun cancel() {
        policyGeneration++
        activeSendRequestId = 0
        activeConfirmRequestId = 0
        _uiState.value = PhoneVerificationUiState()
    }
}
