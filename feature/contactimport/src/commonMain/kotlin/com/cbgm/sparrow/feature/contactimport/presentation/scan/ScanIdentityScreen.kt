package com.cbgm.sparrow.feature.contactimport.presentation.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScanIdentityUiEvent
import com.cbgm.sparrow.feature.media.device.CameraScanner
import com.cbgm.sparrow.feature.media.domain.model.CameraScanMode
import com.cbgm.sparrow.feature.media.domain.model.CameraScanResult
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contactimport_scan_identity_instruction
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScanIdentityScreen(
    onUiEvent: (ScanIdentityUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    CameraScanner(
        mode = CameraScanMode.QrCode,
        onResult = { result ->
            if (result is CameraScanResult.QrCode) {
                onUiEvent(ScanIdentityUiEvent.QrCodeScanned(result.value))
            }
        },
        hint = stringResource(Res.string.feature_contactimport_scan_identity_instruction),
        onDismiss = { onUiEvent(ScanIdentityUiEvent.BackClicked) },
        modifier =
            modifier
                .fillMaxSize()
                .background(FunctionalColors.MediaBackground)
    )
}

@Preview
@Composable
fun ScannedIdentityPreview() {
    SparrowTheme {
        ScanIdentityScreen(
            onUiEvent = {}
        )
    }
}
