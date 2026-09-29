package com.cbgm.sparrow.feature.applock.device

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
actual fun AppLockAuthenticationLauncher(
    requestId: Int,
    enabled: Boolean,
    title: String,
    reason: String,
    onResult: (AppLockAuthenticationResult) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val currentOnResult = rememberUpdatedState(onResult)

    LaunchedEffect(requestId, enabled, activity, title, reason) {
        if (!enabled || requestId <= 0) return@LaunchedEffect

        if (activity == null || !activity.canUseDeviceAuthentication()) {
            currentOnResult.value(AppLockAuthenticationResult.Unavailable)
            return@LaunchedEffect
        }

        currentOnResult.value(
            activity.authenticateDeviceOwner(
                title = title,
                reason = reason
            )
        )
    }
}

private fun Activity.canUseDeviceAuthentication(): Boolean {
    val keyguardManager = getSystemService(KeyguardManager::class.java)
    if (!keyguardManager.isDeviceSecure) return false

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true

    val biometricManager = getSystemService(BiometricManager::class.java)
    val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
}

private suspend fun Activity.authenticateDeviceOwner(
    title: String,
    reason: String
): AppLockAuthenticationResult =
    suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()

        val promptBuilder =
            BiometricPrompt.Builder(this)
                .setTitle(title)
                .setSubtitle(reason)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            promptBuilder.setDeviceCredentialAllowed(true)
        }

        val prompt = promptBuilder.build()

        prompt.authenticate(
            cancellationSignal,
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    if (continuation.isActive) {
                        continuation.resume(AppLockAuthenticationResult.Authenticated)
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    if (!continuation.isActive) return

                    val result =
                        when (errorCode) {
                            BiometricPrompt.BIOMETRIC_ERROR_CANCELED,
                            BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ->
                                AppLockAuthenticationResult.Cancelled

                            BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS,
                            BiometricPrompt.BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL ->
                                AppLockAuthenticationResult.Unavailable

                            else ->
                                AppLockAuthenticationResult.Failed(
                                    message = errString?.toString().orEmpty()
                                )
                        }

                    continuation.resume(result)
                }

                // A non-matching biometric keeps the system prompt open. Do
                // not complete the request here; the user can try again.
                override fun onAuthenticationFailed() = Unit
            }
        )

        continuation.invokeOnCancellation {
            cancellationSignal.cancel()
        }
    }

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
