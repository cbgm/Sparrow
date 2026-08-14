package com.cbgm.sparrow.core.ui.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun BlockScreenshotEffect(enabled: Boolean) {
    val context = LocalContext.current
    val activity =
        remember(context) {
            context.findActivity()
        }

    val window = activity?.window

    DisposableEffect(window, enabled) {
        if (window == null || !enabled) {
            onDispose {}
        } else {
            BlockScreenshotFlagManager.acquire(window)

            onDispose {
                BlockScreenshotFlagManager.release(window)
            }
        }
    }
}

private object BlockScreenshotFlagManager {
    private data class SecureWindowState(
        var referenceCount: Int,
        val wasAlreadySecure: Boolean
    )

    private val windowStates = mutableMapOf<Window, SecureWindowState>()

    fun acquire(window: Window) {
        val existingState = windowStates[window]

        if (existingState != null) {
            existingState.referenceCount++
            return
        }

        val wasAlreadySecure =
            window.attributes.flags and
                WindowManager.LayoutParams.FLAG_SECURE != 0

        windowStates[window] =
            SecureWindowState(
                referenceCount = 1,
                wasAlreadySecure = wasAlreadySecure
            )

        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    fun release(window: Window) {
        val state = windowStates[window] ?: return

        state.referenceCount--

        if (state.referenceCount > 0) {
            return
        }

        windowStates.remove(window)

        if (!state.wasAlreadySecure) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
