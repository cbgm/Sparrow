package com.cbgm.sparrow.feature.chats.presentation.verification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.chats.presentation.verification.model.GroupMemberQrVerificationError
import com.cbgm.sparrow.feature.chats.presentation.verification.model.GroupMemberQrVerificationUiEvent
import com.cbgm.sparrow.feature.contactimport.presentation.component.ScannedIdentityConfirmationDialog
import com.cbgm.sparrow.feature.contactimport.presentation.scan.ScanIdentityRoute
import com.cbgm.sparrow.feature.contactimport.presentation.scan.model.ScanIdentityUiEvent
import com.cbgm.sparrow.feature.contactimport.presentation.verify.component.QrVerificationErrorDialog
import com.cbgm.sparrow.feature.contactimport.presentation.verify.component.QrVerificationProgressDialog
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_group_qr_identity_mismatch
import com.cbgm.sparrow.resources.feature_contactimport_invalid_identity_qr
import com.cbgm.sparrow.resources.feature_contactimport_qr_verification_failed
import com.cbgm.sparrow.resources.feature_contactimport_trust_and_verify
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun GroupMemberVerificationFlow(
    viewModel: GroupMemberQrVerificationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    key(uiState.scanAttempt) {
        ScanIdentityRoute(
            onUiEvent = { event ->
                when (event) {
                    is ScanIdentityUiEvent.QrCodeScanned ->
                        viewModel.onUiEvent(
                            GroupMemberQrVerificationUiEvent.QrCodeScanned(event.encodedIdentity)
                        )

                    ScanIdentityUiEvent.BackClicked ->
                        viewModel.onUiEvent(GroupMemberQrVerificationUiEvent.BackClicked)
                }
            }
        )
    }

    ScannedIdentityConfirmationDialog(
        isVisible = uiState.preview != null,
        preview = uiState.preview,
        confirmButtonText = stringResource(Res.string.feature_contactimport_trust_and_verify),
        onConfirm = { viewModel.onUiEvent(GroupMemberQrVerificationUiEvent.ConfirmClicked) },
        onDismiss = { viewModel.onUiEvent(GroupMemberQrVerificationUiEvent.PreviewDismissed) }
    )

    QrVerificationProgressDialog(isVisible = uiState.isProcessing)

    QrVerificationErrorDialog(
        isVisible = uiState.error != null,
        message =
            when (uiState.error) {
                GroupMemberQrVerificationError.INVALID_QR ->
                    stringResource(Res.string.feature_contactimport_invalid_identity_qr)

                GroupMemberQrVerificationError.IDENTITY_MISMATCH ->
                    stringResource(Res.string.feature_chats_group_qr_identity_mismatch)

                GroupMemberQrVerificationError.VERIFICATION_FAILED ->
                    stringResource(Res.string.feature_contactimport_qr_verification_failed)

                else -> ""
            },
        onRetry = { viewModel.onUiEvent(GroupMemberQrVerificationUiEvent.RetryClicked) },
        onCancel = { viewModel.onUiEvent(GroupMemberQrVerificationUiEvent.BackClicked) }
    )
}
