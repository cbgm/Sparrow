package com.cbgm.sparrow.feature.identity.presentation.verification.model

enum class PhoneVerificationStageUi { HIDDEN, REQUEST, SENDING, CODE, CONFIRMING }

data class PhoneVerificationUiState(
    val stage: PhoneVerificationStageUi = PhoneVerificationStageUi.HIDDEN,
    val phoneNumber: String = "",
    val code: String = "",
    val resendInSeconds: Int = 0,
    val canSkip: Boolean = false
) {
    val isVisible: Boolean
        get() = stage != PhoneVerificationStageUi.HIDDEN

    // "Verify" first requests the SMS, then confirms the complete code.
    val canConfirm: Boolean
        get() =
            stage == PhoneVerificationStageUi.REQUEST ||
                stage == PhoneVerificationStageUi.CODE && code.length == CODE_LENGTH
    val canResend: Boolean
        get() = stage == PhoneVerificationStageUi.CODE && resendInSeconds == 0

    override fun toString(): String = "PhoneVerificationUiState(stage=$stage)"

    companion object {
        /** Firebase SMS codes have six digits. */
        const val CODE_LENGTH = 6
    }
}
