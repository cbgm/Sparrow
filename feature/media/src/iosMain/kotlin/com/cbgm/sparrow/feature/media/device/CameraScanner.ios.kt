package com.cbgm.sparrow.feature.media.device

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureConfig
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.CameraScanMode
import com.cbgm.sparrow.feature.media.domain.model.CameraScanResult

@Composable
actual fun CameraScanner(
    mode: CameraScanMode,
    onResult: (CameraScanResult) -> Unit,
    modifier: Modifier,
    hint: String?,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
) {
    Box(modifier = modifier)
}

@Composable
actual fun rememberCameraScannerLauncher(
    onResult: (CameraScanResult) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): CameraScannerLauncher {
    val currentOnError by rememberUpdatedState(onError)
    var documentMode by remember { mutableStateOf(CameraScanMode.Document()) }
    val cameraLauncher =
        rememberCameraCaptureLauncher(
            config =
                remember(documentMode) {
                    CameraCaptureConfig(
                        allowedTypes = setOf(CameraCaptureType.PHOTO),
                        maxImageDimension = documentMode.maxImageDimension,
                        maxImageBytes = documentMode.maxImageBytes
                    )
                },
            onCaptured = { onResult(CameraScanResult.Document(it)) },
            onDismissed = onDismissed,
            onError = onError
        )
    return remember(cameraLauncher) {
        CameraScannerLauncher(
            launch = { mode ->
                when (mode) {
                    is CameraScanMode.Document -> {
                        documentMode = mode
                        cameraLauncher.launch()
                    }
                    CameraScanMode.QrCode -> currentOnError("QR scanning is not available on iOS yet")
                }
            }
        )
    }
}
