package com.cbgm.sparrow.feature.identity.presentation.verification.model

/** [DAY_FULL]: the phone verifier has no sign-up left today, so no SMS was sent. */
enum class PhoneVerificationStageUi { HIDDEN, REQUEST, SENDING, CODE, CONFIRMING, DAY_FULL }

data class PhoneVerificationUiState(
    val stage: PhoneVerificationStageUi = PhoneVerificationStageUi.HIDDEN,
    val phoneNumber: String = "",
    val code: String = "",
    val resendInSeconds: Int = 0,
    /** How often the code may still be sent again, as the phone verifier allows for this sign-up. */
    val resendsLeft: Int = 0,
    /** With [PhoneVerificationStageUi.DAY_FULL]: when the next day's sign-ups start. */
    val retryAtEpochMilliseconds: Long = 0,
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
        get() = stage == PhoneVerificationStageUi.CODE && resendInSeconds == 0 && resendsLeft > 0

    override fun toString(): String = "PhoneVerificationUiState(stage=$stage)"

    companion object {
        /** Firebase SMS codes have six digits. */
        const val CODE_LENGTH = 6
    }
}
