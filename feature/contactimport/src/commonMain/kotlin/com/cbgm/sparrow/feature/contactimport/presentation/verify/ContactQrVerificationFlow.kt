package com.cbgm.sparrow.feature.contactimport.presentation.verify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.contactimport.presentation.component.ScannedIdentityConfirmationDialog
import com.cbgm.sparrow.feature.contactimport.presentation.verify.component.QrVerificationErrorDialog
import com.cbgm.sparrow.feature.contactimport.presentation.verify.component.QrVerificationProgressDialog
import com.cbgm.sparrow.feature.contactimport.presentation.verify.model.VerifyContactQrUiEvent
import com.cbgm.sparrow.feature.media.presentation.scan.QrScanTile
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_scan_identity_qr
import com.cbgm.sparrow.resources.feature_contactimport_trust_and_verify
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ContactQrVerificationFlow(
    contactId: String,
    enabled: Boolean,
    viewModel: VerifyContactQrViewModel =
        koinViewModel(key = "verify-qr-$contactId") { parametersOf(contactId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    QrScanTile(
        label = stringResource(Res.string.feature_chats_scan_identity_qr),
        enabled = enabled && !uiState.isVerifying,
        retryCount = uiState.scanAttempt,
        onScanned = { value -> viewModel.onUiEvent(VerifyContactQrUiEvent.QrCodeScanned(value)) }
    )

    ScannedIdentityConfirmationDialog(
        isVisible = uiState.scannedIdentityPreview != null,
        preview = uiState.scannedIdentityPreview,
        confirmButtonText = stringResource(Res.string.feature_contactimport_trust_and_verify),
        onConfirm = { viewModel.onUiEvent(VerifyContactQrUiEvent.ScannedIdentityConfirmed) },
        onDismiss = { viewModel.onUiEvent(VerifyContactQrUiEvent.ScannedIdentityDismissed) }
    )

    QrVerificationProgressDialog(isVisible = uiState.isVerifying)

    QrVerificationErrorDialog(
        isVisible = !uiState.errorMessage.isNullOrBlank(),
        message = uiState.errorMessage.orEmpty(),
        onRetry = { viewModel.onUiEvent(VerifyContactQrUiEvent.RetryClicked) },
        onCancel = { viewModel.onUiEvent(VerifyContactQrUiEvent.ErrorDismissed) }
    )
}
