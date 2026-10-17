package com.cbgm.sparrow.feature.identity.device

import androidx.compose.runtime.Composable

sealed interface PhoneVerificationRequest {
    val id: Int

    data class Send(
        override val id: Int,
        val phoneNumber: String
    ) : PhoneVerificationRequest

    data class Confirm(
        override val id: Int,
        val phoneNumber: String,
        val verificationId: String,
        val code: String
    ) : PhoneVerificationRequest
}

sealed interface PhoneVerificationResult {
    val requestId: Int

    data class CodeSent(
        override val requestId: Int,
        val verificationId: String
    ) : PhoneVerificationResult

    data class Verified(
        override val requestId: Int
    ) : PhoneVerificationResult

    data class Failed(
        override val requestId: Int
    ) : PhoneVerificationResult

    data class Unsupported(
        override val requestId: Int
    ) : PhoneVerificationResult
}

/** Android uses Firebase Phone Auth; no verification is simulated or trusted locally. */
@Composable
expect fun PhoneVerificationLauncher(
    request: PhoneVerificationRequest?,
    onResult: (PhoneVerificationResult) -> Unit
)
