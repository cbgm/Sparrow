package com.cbgm.securechat.feature.contactimport.device

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberQrScannerPermissionRequest(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): () -> Unit {
    val context = LocalContext.current

    val currentOnGranted = rememberUpdatedState(newValue = onPermissionGranted)

    val currentOnDenied = rememberUpdatedState(newValue = onPermissionDenied)

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                currentOnGranted.value()
            } else {
                currentOnDenied.value()
            }
        }

    return remember(
        context,
        permissionLauncher
    ) {
        {
            val granted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                currentOnGranted.value()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}
