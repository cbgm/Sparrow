package com.cbgm.sparrow.core.ui.theme

import android.app.UiModeManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/** Android 12+ keeps the app's night mode across starts and uses it for the splash; older versions follow the phone. */
@Composable
actual fun SystemNightMode(mode: AppThemeMode) {
    val context = LocalContext.current
    LaunchedEffect(mode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@LaunchedEffect
        val uiModeManager = context.getSystemService(UiModeManager::class.java) ?: return@LaunchedEffect
        val nightMode =
            when (mode) {
                AppThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                AppThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                AppThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            }
        uiModeManager.setApplicationNightMode(nightMode)
    }
}
