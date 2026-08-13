package com.cbgm.securechat.feature.contacts.presentation.platform

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberDeviceContactsPermissionRequest(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): () -> Unit {
    val context =
        LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }

    return remember(
        context,
        permissionLauncher,
        onPermissionGranted,
        onPermissionDenied
    ) {
        {
            val permissionGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED

            if (permissionGranted) {
                onPermissionGranted()
            } else {
                permissionLauncher.launch(
                    Manifest.permission.READ_CONTACTS
                )
            }
        }
    }
}
