package com.cbgm.sparrow.feature.media.device

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFileAccessLauncher(
    onReturned: (String?) -> Unit,
    onError: (String) -> Unit
): FileAccessLauncher {
    val context = LocalContext.current
    val currentOnReturned = rememberUpdatedState(onReturned)
    val currentOnError = rememberUpdatedState(onError)

    val legacyPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            currentOnReturned.value(null)
        }
    val settingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            currentOnReturned.value(null)
        }

    return remember(context, legacyPermissionLauncher, settingsLauncher) {
        object : FileAccessLauncher {
            override fun launch() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val appIntent =
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    runCatching { settingsLauncher.launch(appIntent) }
                        .recoverCatching {
                            settingsLauncher.launch(
                                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            )
                        }
                        .onFailure { error ->
                            currentOnError.value(error.message ?: "File access settings could not be opened")
                        }
                } else {
                    legacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }
}
