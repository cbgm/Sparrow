package com.cbgm.sparrow.feature.onboarding.device

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun LocalNetworkPermissionRequester(
    needed: Boolean,
    deniedMessage: String
) {
    val context = LocalContext.current
    val currentDeniedMessage by rememberUpdatedState(deniedMessage)
    var asked by remember { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Toast.makeText(context, currentDeniedMessage, Toast.LENGTH_LONG).show()
        }

    LaunchedEffect(needed) {
        val required = Build.VERSION.SDK_INT >= ANDROID_17
        val granted = ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
        if (needed && required && !granted && !asked) {
            asked = true
            launcher.launch(ACCESS_LOCAL_NETWORK)
        }
    }
}

private const val ANDROID_17 = 37
private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
