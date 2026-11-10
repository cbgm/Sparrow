package com.cbgm.sparrow.feature.onboarding.device

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@SuppressLint("BatteryLife")
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
        val request = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            "package:${context.packageName}".toUri()
        )
        runCatching { launcher.launch(request) }.onFailure { onResult(false) }
    }
}

private fun Context.isExempted(): Boolean = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
