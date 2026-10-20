package com.cbgm.sparrow.feature.identity.presentation.verification.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowCodeField
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.device.PhoneVerificationHost
import com.cbgm.sparrow.feature.identity.presentation.verification.PhoneVerificationViewModel
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationResultUi
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationStageUi
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationUiEvent
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_identity_phone_verification_confirm
import com.cbgm.sparrow.resources.feature_identity_phone_verification_hint
import com.cbgm.sparrow.resources.feature_identity_phone_verification_request
import com.cbgm.sparrow.resources.feature_identity_phone_verification_resend
import com.cbgm.sparrow.resources.feature_identity_phone_verification_resend_in
import com.cbgm.sparrow.resources.feature_identity_phone_verification_sending
import com.cbgm.sparrow.resources.feature_identity_phone_verification_skip
import com.cbgm.sparrow.resources.feature_identity_phone_verification_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Verifies [phoneNumber] by SMS when it is set and reports the result through [onResult]. */
@Composable
fun PhoneVerificationDialog(
    phoneNumber: String?,
    onResult: (PhoneVerificationResultUi) -> Unit,
    viewModel: PhoneVerificationViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()

    // Present for the whole screen, so the platform has its screen before a code is requested.
    PhoneVerificationHost()
    LaunchedEffect(phoneNumber) {
        phoneNumber?.let(viewModel::start)
    }
    LaunchedEffect(result) {
        result?.let {
            viewModel.consumeResult()
            onResult(it)
        }
    }
    PhoneVerificationDialogContent(state = state, onUiEvent = viewModel::onUiEvent)
}

@Composable
private fun PhoneVerificationDialogContent(
    state: PhoneVerificationUiState,
    onUiEvent: (PhoneVerificationUiEvent) -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = { onUiEvent(PhoneVerificationUiEvent.DismissClicked) },
        title = stringResource(Res.string.feature_identity_phone_verification_title),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state.stage) {
                    PhoneVerificationStageUi.REQUEST ->
                        Text(
                            text =
                                stringResource(
                                    Res.string.feature_identity_phone_verification_request,
                                    state.phoneNumber
                                )
                        )
                    PhoneVerificationStageUi.SENDING -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text =
                                stringResource(
                                    Res.string.feature_identity_phone_verification_sending,
                                    state.phoneNumber
                                )
                        )
                    }
                    else -> PhoneVerificationCodeInput(state = state, onUiEvent = onUiEvent)
                }
                if (state.canSkip) {
                    TextButton(
                        enabled = state.stage != PhoneVerificationStageUi.CONFIRMING,
                        onClick = { onUiEvent(PhoneVerificationUiEvent.SkipClicked) }
                    ) {
                        Text(stringResource(Res.string.feature_identity_phone_verification_skip))
                    }
                }
            }
        },
        confirmButton = {
            SparrowApprovalButton(
                enabled = state.canConfirm,
                onClick = { onUiEvent(PhoneVerificationUiEvent.ConfirmClicked) },
                text = stringResource(Res.string.feature_identity_phone_verification_confirm),
                fillMaxWidth = false
            )
        },
        dismissButton = {
            SparrowOutlinedButton(
                enabled = state.stage != PhoneVerificationStageUi.CONFIRMING,
                onClick = { onUiEvent(PhoneVerificationUiEvent.DismissClicked) },
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        },
        isVisible = state.isVisible
    )
}

// The code entered by the user, or filled in when the platform read the SMS itself.
@Composable
private fun PhoneVerificationCodeInput(
    state: PhoneVerificationUiState,
    onUiEvent: (PhoneVerificationUiEvent) -> Unit
) {
    Text(
        text =
            stringResource(
                Res.string.feature_identity_phone_verification_hint,
                state.phoneNumber
            )
    )
    SparrowCodeField(
        value = state.code,
        onValueChange = { onUiEvent(PhoneVerificationUiEvent.CodeChanged(it)) },
        length = PhoneVerificationUiState.CODE_LENGTH,
        isEnabled = state.stage == PhoneVerificationStageUi.CODE
    )
    TextButton(
        enabled = state.canResend,
        onClick = { onUiEvent(PhoneVerificationUiEvent.ResendClicked) }
    ) {
        Text(
            text =
                if (state.resendInSeconds > 0) {
                    stringResource(
                        Res.string.feature_identity_phone_verification_resend_in,
                        state.resendInSeconds
                    )
                } else {
                    stringResource(
                        Res.string.feature_identity_phone_verification_resend
                    )
                }
        )
    }
}
