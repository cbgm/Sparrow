package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureConfig
import com.cbgm.sparrow.feature.media.domain.model.CapturedMedia

data class CameraCaptureLauncher(
    val launch: () -> Unit
)

@Composable
expect fun rememberCameraCaptureLauncher(
    config: CameraCaptureConfig,
    onCaptured: (CapturedMedia) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): CameraCaptureLauncher
