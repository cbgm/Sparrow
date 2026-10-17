package com.cbgm.sparrow.feature.identity.presentation.verification.model

import com.cbgm.sparrow.feature.identity.device.PhoneVerificationRequest

enum class PhoneVerificationStage { IDLE, LOADING_POLICY, READY, SENDING_CODE, ENTER_CODE, CHECKING_CODE, VERIFIED }

data class PhoneVerificationUiState(
    val stage: PhoneVerificationStage = PhoneVerificationStage.IDLE,
    val phoneNumber: String = "",
    val required: Boolean = true,
    val code: String = "",
    val verificationId: String = "",
    val request: PhoneVerificationRequest? = null,
    val hasError: Boolean = false,
    val unsupported: Boolean = false
) {
    val canConfirm: Boolean get() = stage == PhoneVerificationStage.ENTER_CODE && code.length >= 6
    val canSkip: Boolean get() = !required && stage != PhoneVerificationStage.VERIFIED

    override fun toString(): String = "PhoneVerificationUiState(stage=$stage, redacted)"
}
