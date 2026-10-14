package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.feature.media.domain.model.CameraScanMode
import com.cbgm.sparrow.feature.media.domain.model.CameraScanResult

data class CameraScannerLauncher(
    val launch: (CameraScanMode) -> Unit
)

@Composable
expect fun CameraScanner(
    mode: CameraScanMode,
    onResult: (CameraScanResult) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    onDismiss: () -> Unit = {},
    onError: (String) -> Unit = {}
)

@Composable
expect fun rememberCameraScannerLauncher(
    onResult: (CameraScanResult) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): CameraScannerLauncher
