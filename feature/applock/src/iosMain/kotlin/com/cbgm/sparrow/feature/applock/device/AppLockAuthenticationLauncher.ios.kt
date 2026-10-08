package com.cbgm.sparrow.feature.applock.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import kotlin.coroutines.resume

@Composable
actual fun AppLockAuthenticationLauncher(
    requestId: Int,
    enabled: Boolean,
    title: String,
    reason: String,
    onResult: (AppLockAuthenticationResult) -> Unit
) {
    val currentOnResult = rememberUpdatedState(onResult)

    LaunchedEffect(requestId, enabled, title, reason) {
        if (!enabled || requestId <= 0) return@LaunchedEffect

        currentOnResult.value(
            authenticateDeviceOwner(reason = reason)
        )
    }
}

private suspend fun authenticateDeviceOwner(reason: String): AppLockAuthenticationResult =
    suspendCancellableCoroutine { continuation ->
        val context = LAContext()

        if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)) {
            continuation.resume(AppLockAuthenticationResult.Unavailable)
            return@suspendCancellableCoroutine
        }

        context.evaluatePolicy(
            LAPolicyDeviceOwnerAuthentication,
            reason
        ) { success, _ ->
            if (!continuation.isActive) return@evaluatePolicy

            if (success) {
                continuation.resume(AppLockAuthenticationResult.Authenticated)
            } else {
                // LocalAuthentication owns the retry/passcode flow. A final
                // unsuccessful reply leaves Sparrow locked and lets the user
                // explicitly try again from the app-lock screen.
                continuation.resume(AppLockAuthenticationResult.Cancelled)
            }
        }

        continuation.invokeOnCancellation {
            context.invalidate()
        }
    }
