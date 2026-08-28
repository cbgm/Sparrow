package com.cbgm.sparrow.feature.media.device

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (ByteArray) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): ImagePickerLauncher {
    val context = LocalContext.current
    val currentOnImageSelected = rememberUpdatedState(onImageSelected)
    val currentOnDismissed = rememberUpdatedState(onDismissed)
    val currentOnError = rememberUpdatedState(onError)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) {
                currentOnDismissed.value()
                return@rememberLauncherForActivityResult
            }

            runCatching {
                requireNotNull(
                    decodeProfilePictureBitmap(
                        context = context,
                        uri = uri
                    )
                ) { "Selected image could not be decoded" }
            }.map(::encodeProfilePictureSource)
                .onSuccess(currentOnImageSelected.value)
                .onFailure { error ->
                    currentOnError.value(error.message ?: "Selected image could not be read")
                }
        }

    return remember(launcher) {
        ImagePickerLauncher(
            launch = { launcher.launch("image/*") }
        )
    }
}
