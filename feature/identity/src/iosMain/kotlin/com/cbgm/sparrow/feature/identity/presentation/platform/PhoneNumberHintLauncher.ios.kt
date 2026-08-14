package com.cbgm.sparrow.feature.identity.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun PhoneNumberHintLauncher(
    requestId: Int,
    enabled: Boolean,
    onResult: (PhoneNumberHintResult) -> Unit
) {
    LaunchedEffect(
        requestId,
        enabled
    ) {
        if (enabled && requestId > 0) {
            /*
             * iOS has no public API equivalent to Android's Phone
             * Number Hint picker. The shared onboarding UI keeps its
             * manual-entry fallback visible.
             */
            onResult(
                PhoneNumberHintResult.Unavailable
            )
        }
    }
}
