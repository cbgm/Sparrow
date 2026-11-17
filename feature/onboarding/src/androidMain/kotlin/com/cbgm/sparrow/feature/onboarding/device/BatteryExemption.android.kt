package com.cbgm.sparrow.feature.onboarding.device

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Opens Android's battery optimization list, where the user sets Sparrow to "Not optimized". Google Play allows the
 * direct exemption dialog only for apps that technically cannot use its push service; Sparrow does without it by
 * choice, so it uses the list every app may open.
 */
@Composable
actual fun BatteryExemptionRequester(
    requestId: Int,
    onResult: (exempted: Boolean) -> Unit
) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onResult(context.isExempted())
        }

    LaunchedEffect(requestId) {
        if (requestId <= 0) return@LaunchedEffect
        if (context.isExempted()) {
            onResult(true)
            return@LaunchedEffect
        }
        runCatching { launcher.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }.onFailure { onResult(false) }
    }
}

private fun Context.isExempted(): Boolean = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
