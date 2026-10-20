package com.cbgm.sparrow.feature.identity.presentation.verification.model

sealed interface PhoneVerificationResultUi {
    /** The number is verified (or a debug build skipped the check). */
    data object Verified : PhoneVerificationResultUi

    data object Cancelled : PhoneVerificationResultUi
}
