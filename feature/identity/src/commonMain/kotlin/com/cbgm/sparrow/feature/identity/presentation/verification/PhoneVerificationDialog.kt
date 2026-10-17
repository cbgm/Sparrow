package com.cbgm.sparrow.feature.identity.presentation.verification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationStage
import com.cbgm.sparrow.feature.identity.presentation.verification.model.PhoneVerificationUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.base_retry
import com.cbgm.sparrow.resources.base_verify
import com.cbgm.sparrow.resources.feature_identity_phone_verification_code
import com.cbgm.sparrow.resources.feature_identity_phone_verification_description
import com.cbgm.sparrow.resources.feature_identity_phone_verification_error
import com.cbgm.sparrow.resources.feature_identity_phone_verification_send_code
import com.cbgm.sparrow.resources.feature_identity_phone_verification_skip
import com.cbgm.sparrow.resources.feature_identity_phone_verification_title
import com.cbgm.sparrow.resources.feature_identity_phone_verification_unsupported
import org.jetbrains.compose.resources.stringResource

@Composable
fun PhoneVerificationDialog(
    state: PhoneVerificationUiState,
    onCodeChanged: (String) -> Unit,
    onSendCode: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit,
    isVisible: Boolean
) {
    SparrowAlertDialog(
        isVisible = isVisible,
        onDismissRequest = onCancel,
        title = stringResource(Res.string.feature_identity_phone_verification_title),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(
                        Res.string.feature_identity_phone_verification_description,
                        state.phoneNumber
                    )
                )
                when (state.stage) {
                    PhoneVerificationStage.LOADING_POLICY,
                    PhoneVerificationStage.SENDING_CODE,
                    PhoneVerificationStage.CHECKING_CODE -> CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.Base.progressFieldIndicatorSize),
                        strokeWidth = Dimens.Base.progressIndicatorStrokeWidth,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    PhoneVerificationStage.ENTER_CODE -> OutlinedTextField(
                        value = state.code,
                        onValueChange = onCodeChanged,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        label = { Text(stringResource(Res.string.feature_identity_phone_verification_code)) }
                    )

                    else -> Unit
                }
                val errorText = when {
                    state.hasError -> stringResource(Res.string.feature_identity_phone_verification_error)
                    state.unsupported -> stringResource(Res.string.feature_identity_phone_verification_unsupported)
                    else -> null
                }
                errorText?.let { text ->
                    Text(
                        text = text,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            SparrowApprovalButton(
                enabled = state.hasError || (
                    !state.unsupported && (state.stage == PhoneVerificationStage.READY || state.canConfirm)
                ),
                onClick = when {
                    state.hasError -> onRetry
                    state.stage == PhoneVerificationStage.READY -> onSendCode
                    else -> onConfirm
                },
                text = when {
                    state.hasError -> stringResource(Res.string.base_retry)
                    state.stage == PhoneVerificationStage.READY -> stringResource(Res.string.feature_identity_phone_verification_send_code)
                    else -> stringResource(Res.string.base_verify)
                },
                fillMaxWidth = false
            )
        },
        dismissButton = {
            SparrowOutlinedButton(
                onClick = onCancel,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
            SparrowOutlinedButton(
                onClick = onSkip,
                enabled = state.canSkip,
                fillMaxWidth = false,
                text = stringResource(Res.string.feature_identity_phone_verification_skip)
            )
        }
    )
}
