package com.cbgm.sparrow.feature.contactimport.presentation.verify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.extensions.toFingerprint
import com.cbgm.sparrow.feature.contactimport.presentation.component.ScannedIdentityConfirmationDialog
import com.cbgm.sparrow.feature.contactimport.presentation.component.verification.QrVerificationErrorDialog
import com.cbgm.sparrow.feature.contactimport.presentation.component.verification.QrVerificationProgressDialog
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScanIdentityUiEvent
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScannedIdentityPreview
import com.cbgm.sparrow.feature.contactimport.presentation.verify.model.VerifyContactQrUiEvent
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityShareRepository
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contactimport_invalid_identity_qr
import com.cbgm.sparrow.resources.feature_contactimport_trust_and_verify
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactQrVerificationFlow(
    viewModel: VerifyContactQrViewModel = koinViewModel(),
    identityShareRepository: IdentityShareRepository = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var scanAttempt by remember { mutableIntStateOf(0) }
    var scannedIdentityPreview by remember { mutableStateOf<ScannedIdentityPreview?>(null) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    val invalidIdentityQrMessage =
        stringResource(Res.string.feature_contactimport_invalid_identity_qr)

    key(scanAttempt) {
        ScanIdentityRoute(
            onUiEvent = { event ->
                when (event) {
                    is ScanIdentityUiEvent.QrCodeScanned -> {
                        val encodedIdentity = event.encodedIdentity
                        identityShareRepository
                            .decode(encodedValue = encodedIdentity)
                            .onSuccess { payload ->
                                scannedIdentityPreview =
                                    ScannedIdentityPreview(
                                        encodedIdentity = encodedIdentity,
                                        displayName = payload.contactDetails.displayName,
                                        phoneNumber = payload.contactDetails.phoneNumber,
                                        signingKeyFingerprint = payload.signingPublicKey.toFingerprint(),
                                        encryptionKeyFingerprint = payload.encryptionPublicKey.toFingerprint()
                                    )
                            }.onFailure { error ->
                                scanErrorMessage = error.message ?: invalidIdentityQrMessage
                            }
                    }

                    ScanIdentityUiEvent.BackClicked ->
                        viewModel.onUiEvent(VerifyContactQrUiEvent.BackClicked)
                }
            }
        )
    }

    scannedIdentityPreview?.let { preview ->
        ScannedIdentityConfirmationDialog(
            preview = preview,
            confirmButtonText = stringResource(Res.string.feature_contactimport_trust_and_verify),
            onConfirm = {
                scannedIdentityPreview = null
                viewModel.onUiEvent(VerifyContactQrUiEvent.QrCodeScanned(preview.encodedIdentity))
            },
            onDismiss = {
                scannedIdentityPreview = null
                scanAttempt++
            }
        )
    }

    if (uiState.isVerifying) {
        QrVerificationProgressDialog()
    }

    val verificationErrorMessage = scanErrorMessage ?: uiState.errorMessage

    verificationErrorMessage?.let { errorMessage ->
        QrVerificationErrorDialog(
            message = errorMessage,
            onRetry = {
                scanErrorMessage = null
                viewModel.onUiEvent(VerifyContactQrUiEvent.ErrorDismissed)
                scanAttempt++
            },
            onCancel = { viewModel.onUiEvent(VerifyContactQrUiEvent.BackClicked) }
        )
    }
}
