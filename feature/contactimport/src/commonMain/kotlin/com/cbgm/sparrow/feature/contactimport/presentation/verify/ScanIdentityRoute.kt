package com.cbgm.sparrow.feature.contactimport.presentation.verify

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.contactimport.device.rememberQrScannerPermissionRequest
import com.cbgm.sparrow.feature.contactimport.presentation.scan.ScanIdentityScreen
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScanIdentityUiEvent
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_back
import com.cbgm.sparrow.resources.feature_contactimport_camera_permission_required
import com.cbgm.sparrow.resources.feature_contactimport_grant_camera_permission
import org.jetbrains.compose.resources.stringResource

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

    when {
        cameraPermissionGranted -> {
            ScanIdentityScreen(
                onUiEvent = onUiEvent
            )
        }

        cameraPermissionDenied -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = stringResource(Res.string.feature_contactimport_camera_permission_required))

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                Button(
                    onClick = requestCameraPermission
                ) {
                    Text(stringResource(Res.string.feature_contactimport_grant_camera_permission))
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

                Button(
                    onClick = { onUiEvent(ScanIdentityUiEvent.BackClicked) }
                ) {
                    Text(stringResource(Res.string.base_back))
                }
            }
        }
    }
}
