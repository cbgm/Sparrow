package com.cbgm.sparrow.feature.contactimport.presentation.verify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.contactimport.presentation.component.ScannedIdentityConfirmationDialog
import com.cbgm.sparrow.feature.contactimport.presentation.scan.ScanIdentityRoute
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScanIdentityUiEvent
import com.cbgm.sparrow.feature.contactimport.presentation.verify.component.QrVerificationErrorDialog
import com.cbgm.sparrow.feature.contactimport.presentation.verify.component.QrVerificationProgressDialog
import com.cbgm.sparrow.feature.contactimport.presentation.verify.model.VerifyContactQrUiEvent
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contactimport_trust_and_verify
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactQrVerificationFlow(
    viewModel: VerifyContactQrViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var scanAttempt by remember { mutableIntStateOf(0) }

    key(scanAttempt) {
        ScanIdentityRoute(
            onUiEvent = { event ->
                when (event) {
                    is ScanIdentityUiEvent.QrCodeScanned ->
                        viewModel.onUiEvent(
                            VerifyContactQrUiEvent.QrCodeScanned(event.encodedIdentity)
                        )

                    ScanIdentityUiEvent.BackClicked ->
                        viewModel.onUiEvent(VerifyContactQrUiEvent.BackClicked)
                }
            }
        )
    }

    uiState.scannedIdentityPreview?.let { preview ->
        ScannedIdentityConfirmationDialog(
            preview = preview,
            confirmButtonText = stringResource(Res.string.feature_contactimport_trust_and_verify),
            onConfirm = {
                viewModel.onUiEvent(VerifyContactQrUiEvent.ScannedIdentityConfirmed)
            },
            onDismiss = {
                viewModel.onUiEvent(VerifyContactQrUiEvent.ScannedIdentityDismissed)
                scanAttempt++
            }
        )
    }

    if (uiState.isVerifying) {
        QrVerificationProgressDialog()
    }

    uiState.errorMessage?.let { errorMessage ->
        QrVerificationErrorDialog(
            message = errorMessage,
            onRetry = {
                viewModel.onUiEvent(VerifyContactQrUiEvent.ErrorDismissed)
                scanAttempt++
            },
            onCancel = { viewModel.onUiEvent(VerifyContactQrUiEvent.BackClicked) }
        )
    }
}
