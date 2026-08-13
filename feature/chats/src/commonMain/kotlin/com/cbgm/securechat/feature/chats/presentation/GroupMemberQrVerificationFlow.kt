package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberQrVerificationError
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberQrVerificationUiEvent
import com.cbgm.securechat.feature.chats.presentation.screen.details.GroupMemberQrVerificationViewModel
import com.cbgm.securechat.feature.contactimport.presentation.component.verification.QrVerificationErrorDialog
import com.cbgm.securechat.feature.contactimport.presentation.component.verification.QrVerificationProgressDialog
import com.cbgm.securechat.feature.contactimport.presentation.scan.model.ScanIdentityUiEvent
import com.cbgm.securechat.feature.contactimport.presentation.screen.component.ScannedIdentityConfirmationDialog
import com.cbgm.securechat.feature.contactimport.presentation.verify.ScanIdentityRoute
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_group_qr_identity_mismatch
import com.cbgm.securechat.resources.feature_contactimport_invalid_identity_qr
import com.cbgm.securechat.resources.feature_contactimport_qr_verification_failed
import com.cbgm.securechat.resources.feature_contactimport_trust_and_verify
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun GroupMemberQrVerificationFlow(
    groupId: String,
    contactId: String,
    viewModel: GroupMemberQrVerificationViewModel =
        koinViewModel {
            parametersOf(groupId, contactId)
        }
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

    uiState.preview?.let { preview ->
        ScannedIdentityConfirmationDialog(
            preview = preview,
            confirmButtonText = stringResource(Res.string.feature_contactimport_trust_and_verify),
            onConfirm = { viewModel.onUiEvent(GroupMemberQrVerificationUiEvent.ConfirmClicked) },
            onDismiss = { viewModel.onUiEvent(GroupMemberQrVerificationUiEvent.PreviewDismissed) }
        )
    }

    if (uiState.isProcessing) {
        QrVerificationProgressDialog()
    }

    uiState.error?.let { error ->
        QrVerificationErrorDialog(
            message =
                when (error) {
                    GroupMemberQrVerificationError.INVALID_QR ->
                        stringResource(Res.string.feature_contactimport_invalid_identity_qr)

                    GroupMemberQrVerificationError.IDENTITY_MISMATCH ->
                        stringResource(Res.string.feature_chats_group_qr_identity_mismatch)

                    GroupMemberQrVerificationError.VERIFICATION_FAILED ->
                        stringResource(Res.string.feature_contactimport_qr_verification_failed)
                },
            onRetry = { viewModel.onUiEvent(GroupMemberQrVerificationUiEvent.RetryClicked) },
            onCancel = { viewModel.onUiEvent(GroupMemberQrVerificationUiEvent.BackClicked) }
        )
    }
}
