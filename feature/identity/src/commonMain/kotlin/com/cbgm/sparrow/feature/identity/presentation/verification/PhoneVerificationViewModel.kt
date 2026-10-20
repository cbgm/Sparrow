package com.cbgm.sparrow.feature.identity.presentation.verification

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.core.util.flow.countdown
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.identity.domain.model.PhoneVerificationStep
import com.cbgm.sparrow.feature.identity.domain.usecase.ConfirmPhoneVerificationCodeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.SendPhoneVerificationCodeUseCase
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationResultUi
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationStageUi
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationUiEvent
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_identity_phone_verification_send_failed
import com.cbgm.sparrow.resources.feature_identity_phone_verification_wrong_code
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * The SMS verification shown for "Approve and create" and "Restore". It only verifies: the
 * requesting screen stores its data once [result] is [PhoneVerificationResultUi.Verified].
 */
class PhoneVerificationViewModel(
    private val sendCode: SendPhoneVerificationCodeUseCase,
    private val confirmCode: ConfirmPhoneVerificationCodeUseCase,
    private val isDebugBuild: Boolean
) : BaseViewModel() {
    private val logger = SparrowLog.withTag("PhoneVerification")
    private val _uiState = MutableStateFlow(PhoneVerificationUiState())
    val uiState: StateFlow<PhoneVerificationUiState> = _uiState.asStateFlow()
    private val _result = MutableStateFlow<PhoneVerificationResultUi?>(null)
    val result: StateFlow<PhoneVerificationResultUi?> = _result.asStateFlow()

    // Work of the open dialog; closing cancels it, so answers of an abandoned request are dropped.
    private var dialogScope: CoroutineScope? = null

    private var sending: Job? = null
    private var verificationId: String? = null

    /** Opens the dialog for the normalized [phoneNumber]; the SMS is sent on "Verify". */
    fun start(phoneNumber: String) {
        if (_uiState.value.isVisible) return
        val parent = viewModelScope.coroutineContext
        dialogScope = CoroutineScope(parent + SupervisorJob(parent.job))
        _uiState.value =
            PhoneVerificationUiState(
                stage = PhoneVerificationStageUi.REQUEST,
                phoneNumber = phoneNumber,
                canSkip = isDebugBuild
            )
    }

    fun consumeResult() {
        _result.value = null
    }

    fun onUiEvent(event: PhoneVerificationUiEvent) {
        when (event) {
            is PhoneVerificationUiEvent.CodeChanged -> enterCode(event.value)
            PhoneVerificationUiEvent.ConfirmClicked -> verify()
            PhoneVerificationUiEvent.ResendClicked -> resend()
            PhoneVerificationUiEvent.SkipClicked -> skip()
            PhoneVerificationUiEvent.DismissClicked -> dismiss()
        }
    }

    private fun enterCode(value: String) {
        if (_uiState.value.stage != PhoneVerificationStageUi.CODE) return
        val code = value.filter(Char::isDigit).take(PhoneVerificationUiState.CODE_LENGTH)
        _uiState.update { it.copy(code = code) }
        // The last digit confirms right away
        verify()
    }

    private fun verify() {
        val state = _uiState.value
        if (!state.canConfirm) return
        if (state.stage == PhoneVerificationStageUi.REQUEST) send() else confirm()
    }

    private fun resend() {
        if (_uiState.value.canResend) send()
    }

    // Debug builds only: continue with the entered number and without a Firebase token.
    private fun skip() {
        val state = _uiState.value
        if (state.canSkip && state.stage != PhoneVerificationStageUi.CONFIRMING) {
            finish(PhoneVerificationResultUi.Verified)
        }
    }

    private fun dismiss() {
        if (_uiState.value.stage != PhoneVerificationStageUi.CONFIRMING) {
            finish(PhoneVerificationResultUi.Cancelled)
        }
    }

    private fun send() {
        val scope = dialogScope ?: return
        val phoneNumber = _uiState.value.phoneNumber
        _uiState.update { it.copy(stage = PhoneVerificationStageUi.SENDING, code = "") }
        sending?.cancel()
        sending =
            scope.launch {
                sendCode(phoneNumber).collect { result ->
                    result.onSuccess(::onStep).onFailure { error ->
                        logger.warn { "Verification code could not be sent: ${error.message}" }
                        // Back to "Verify", so sending can be retried (or skipped in debug builds).
                        _uiState.update {
                            it.copy(stage = PhoneVerificationStageUi.REQUEST, code = "")
                        }
                        showError(Res.string.feature_identity_phone_verification_send_failed)
                    }
                }
            }
    }

    private fun onStep(step: PhoneVerificationStep) {
        when (step) {
            is PhoneVerificationStep.CodeSent -> codeSent(step.verificationId)
            is PhoneVerificationStep.CodeReceived -> codeReceived(step.code)
            is PhoneVerificationStep.Verified -> finish(PhoneVerificationResultUi.Verified)
        }
    }

    private fun codeSent(id: String) {
        verificationId = id
        if (_uiState.value.stage != PhoneVerificationStageUi.SENDING) return
        _uiState.update { it.copy(stage = PhoneVerificationStageUi.CODE) }
        dialogScope?.launch {
            countdown(RESEND_DELAY_SECONDS).collect { seconds ->
                _uiState.update { it.copy(resendInSeconds = seconds) }
            }
        }
    }

    // The platform reads the SMS itself (in test mode Firebase supplies the test code).
    private fun codeReceived(code: String?) {
        _uiState.update {
            it.copy(stage = PhoneVerificationStageUi.CONFIRMING, code = code.orEmpty())
        }
    }

    private fun confirm() {
        val id = verificationId ?: return
        val code = _uiState.value.code
        _uiState.update { it.copy(stage = PhoneVerificationStageUi.CONFIRMING) }
        dialogScope?.launch {
            confirmCode(id, code)
                .onSuccess { finish(PhoneVerificationResultUi.Verified) }
                .onFailure { error ->
                    logger.warn { "Verification code was not accepted: ${error.message}" }
                    _uiState.update { it.copy(stage = PhoneVerificationStageUi.CODE, code = "") }
                    showError(Res.string.feature_identity_phone_verification_wrong_code)
                }
        }
    }

    private fun finish(result: PhoneVerificationResultUi) {
        dialogScope?.cancel()
        dialogScope = null
        sending = null
        verificationId = null
        _uiState.value = PhoneVerificationUiState()
        _result.value = result
    }

    private suspend fun showError(resource: StringResource) {
        SparrowLog.showError(getString(resource))
    }

    private companion object {
        const val RESEND_DELAY_SECONDS = 60
    }
}
