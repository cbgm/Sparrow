package com.cbgm.sparrow.feature.applock.device

import androidx.compose.runtime.Composable

sealed interface AppLockAuthenticationResult {
    data object Authenticated : AppLockAuthenticationResult

    data object Cancelled : AppLockAuthenticationResult

    data object Unavailable : AppLockAuthenticationResult

    data class Failed(
        val message: String
    ) : AppLockAuthenticationResult
}

/**
 * Starts platform owner authentication whenever [requestId] changes while
 * [enabled] is true.
 *
 * Android uses the system biometric prompt with device-credential fallback.
 * iOS uses LocalAuthentication with Face ID/Touch ID and passcode fallback.
 */
@Composable
expect fun AppLockAuthenticationLauncher(
    requestId: Int,
    enabled: Boolean,
    title: String,
    reason: String,
    onResult: (AppLockAuthenticationResult) -> Unit
)
