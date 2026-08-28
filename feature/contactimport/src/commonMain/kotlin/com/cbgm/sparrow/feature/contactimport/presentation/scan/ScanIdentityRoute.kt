package com.cbgm.sparrow.feature.contactimport.presentation.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cbgm.sparrow.feature.contactimport.device.rememberQrScannerPermissionRequest
import com.cbgm.sparrow.feature.contactimport.presentation.scan.component.CameraPermissionDialog
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScanIdentityUiEvent

@Composable
fun ScanIdentityRoute(
    onUiEvent: (ScanIdentityUiEvent) -> Unit
) {
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var cameraPermissionDenied by remember { mutableStateOf(false) }

    val requestCameraPermission =
        rememberQrScannerPermissionRequest(
            onPermissionGranted = {
                cameraPermissionGranted = true
                cameraPermissionDenied = false
            },
            onPermissionDenied = {
                cameraPermissionDenied = true
            }
        )

    LaunchedEffect(Unit) {
        requestCameraPermission()
    }

    if (cameraPermissionGranted) {
        ScanIdentityScreen(
            onUiEvent = onUiEvent
        )
    }

    if (cameraPermissionDenied) {
        CameraPermissionDialog(
            onGrantPermission = requestCameraPermission,
            onBack = { onUiEvent(ScanIdentityUiEvent.BackClicked) }
        )
    }
}
