package com.cbgm.sparrow.feature.chats.presentation.verification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.chats.presentation.verification.model.GroupMemberQrVerificationError
import com.cbgm.sparrow.feature.chats.presentation.verification.model.GroupMemberQrVerificationUiEvent
import com.cbgm.sparrow.feature.contactimport.presentation.component.ScannedIdentityConfirmationDialog
import com.cbgm.sparrow.feature.contactimport.presentation.verify.component.QrVerificationErrorDialog
import com.cbgm.sparrow.feature.contactimport.presentation.verify.component.QrVerificationProgressDialog
import com.cbgm.sparrow.feature.media.presentation.scan.QrScanTile
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_group_qr_identity_mismatch
import com.cbgm.sparrow.resources.feature_chats_scan_identity_qr
import com.cbgm.sparrow.resources.feature_contactimport_invalid_identity_qr
import com.cbgm.sparrow.resources.feature_contactimport_qr_verification_failed
import com.cbgm.sparrow.resources.feature_contactimport_trust_and_verify
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun GroupMemberVerificationFlow(
    groupId: String,
    contactId: String,
    enabled: Boolean,
    onVerified: () -> Unit,
    viewModel: GroupMemberQrVerificationViewModel =
        koinViewModel(key = "verify-qr-$groupId-$contactId") { parametersOf(groupId, contactId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnVerified by rememberUpdatedState(onVerified)

    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            currentOnVerified()
        }
    }

    QrScanTile(
        label = stringResource(Res.string.feature_chats_scan_identity_qr),
        enabled = enabled && !uiState.isProcessing,
        retryCount = uiState.scanAttempt,
        onScanned = { value -> viewModel.onUiEvent(GroupMemberQrVerificationUiEvent.QrCodeScanned(value)) }
    )

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
        message = uiState.error?.let { error -> stringResource(error.message()) }.orEmpty(),
        onRetry = { viewModel.onUiEvent(GroupMemberQrVerificationUiEvent.RetryClicked) },
        onCancel = { viewModel.onUiEvent(GroupMemberQrVerificationUiEvent.ErrorDismissed) }
    )
}

private fun GroupMemberQrVerificationError.message() =
    when (this) {
        GroupMemberQrVerificationError.INVALID_QR -> Res.string.feature_contactimport_invalid_identity_qr
        GroupMemberQrVerificationError.IDENTITY_MISMATCH -> Res.string.feature_chats_group_qr_identity_mismatch
        GroupMemberQrVerificationError.VERIFICATION_FAILED -> Res.string.feature_contactimport_qr_verification_failed
    }
