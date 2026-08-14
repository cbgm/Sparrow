package com.cbgm.sparrow.feature.identity.presentation.platform

import androidx.compose.runtime.Composable

sealed interface PhoneNumberHintResult {
    data class Selected(
        val phoneNumber: String
    ) : PhoneNumberHintResult

    data object Unavailable :
        PhoneNumberHintResult

    data object Cancelled :
        PhoneNumberHintResult

    data class Failed(
        val message: String
    ) : PhoneNumberHintResult
}

/**
 * Launches the best phone-number acquisition flow available on the
 * current platform whenever [requestId] changes while [enabled] is true.
 *
 * Android uses Google Play services Phone Number Hint.
 * iOS reports [PhoneNumberHintResult.Unavailable], so the shared manual
 * entry remains available.
 */
@Composable
expect fun PhoneNumberHintLauncher(
    requestId: Int,
    enabled: Boolean,
    onResult: (PhoneNumberHintResult) -> Unit
)
