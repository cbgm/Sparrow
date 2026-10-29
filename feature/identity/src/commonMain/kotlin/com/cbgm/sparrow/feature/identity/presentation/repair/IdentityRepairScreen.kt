package com.cbgm.sparrow.feature.identity.presentation.repair

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.repair.model.IdentityRepairStageUi
import com.cbgm.sparrow.feature.identity.presentation.repair.model.IdentityRepairUiEvent
import com.cbgm.sparrow.feature.identity.presentation.repair.model.IdentityRepairUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_identity_backup_password
import com.cbgm.sparrow.resources.feature_identity_repair_message
import com.cbgm.sparrow.resources.feature_identity_repair_password_hint
import com.cbgm.sparrow.resources.feature_identity_repair_reset
import com.cbgm.sparrow.resources.feature_identity_repair_reset_confirm
import com.cbgm.sparrow.resources.feature_identity_repair_reset_message
import com.cbgm.sparrow.resources.feature_identity_repair_restore
import com.cbgm.sparrow.resources.feature_identity_repair_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun IdentityRepairScreen(
    uiState: IdentityRepairUiState,
    onUiEvent: (IdentityRepairUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(MaterialTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.feature_identity_repair_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(Res.string.feature_identity_repair_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (uiState.isBusy) {
            CircularProgressIndicator()
        }
        SparrowApprovalButton(
            onClick = { onUiEvent(IdentityRepairUiEvent.RestoreClicked) },
            text = stringResource(Res.string.feature_identity_repair_restore),
            enabled = !uiState.isBusy
        )
        SparrowOutlinedButton(
            onClick = { onUiEvent(IdentityRepairUiEvent.ResetClicked) },
            text = stringResource(Res.string.feature_identity_repair_reset),
            enabled = !uiState.isBusy
        )
    }

    IdentityRepairPasswordDialog(uiState, onUiEvent)
    IdentityResetConfirmDialog(uiState, onUiEvent)
}

@Composable
private fun IdentityRepairPasswordDialog(
    uiState: IdentityRepairUiState,
    onUiEvent: (IdentityRepairUiEvent) -> Unit
) {
    SparrowAlertDialog(
        isVisible = uiState.isPasswordDialogVisible,
        onDismissRequest = { onUiEvent(IdentityRepairUiEvent.PasswordDismissed) },
        title = stringResource(Res.string.feature_identity_repair_restore),
        text = {
            Column {
                Text(stringResource(Res.string.feature_identity_repair_password_hint))
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { onUiEvent(IdentityRepairUiEvent.PasswordChanged(it)) },
                    label = { Text(stringResource(Res.string.feature_identity_backup_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            SparrowApprovalButton(
                enabled = uiState.canConfirmPassword,
                onClick = { onUiEvent(IdentityRepairUiEvent.PasswordConfirmed) },
                text = stringResource(Res.string.feature_identity_repair_restore),
                fillMaxWidth = false
            )
        },
        dismissButton = {
            SparrowOutlinedButton(
                onClick = { onUiEvent(IdentityRepairUiEvent.PasswordDismissed) },
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}

@Composable
private fun IdentityResetConfirmDialog(
    uiState: IdentityRepairUiState,
    onUiEvent: (IdentityRepairUiEvent) -> Unit
) {
    SparrowAlertDialog(
        isVisible = uiState.stage == IdentityRepairStageUi.CONFIRM_RESET,
        onDismissRequest = { onUiEvent(IdentityRepairUiEvent.ResetDismissed) },
        title = stringResource(Res.string.feature_identity_repair_reset),
        text = { Text(stringResource(Res.string.feature_identity_repair_reset_message)) },
        confirmButton = {
            SparrowApprovalButton(
                onClick = { onUiEvent(IdentityRepairUiEvent.ResetConfirmed) },
                text = stringResource(Res.string.feature_identity_repair_reset_confirm),
                fillMaxWidth = false
            )
        },
        dismissButton = {
            SparrowOutlinedButton(
                onClick = { onUiEvent(IdentityRepairUiEvent.ResetDismissed) },
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}

@Preview
@Composable
private fun IdentityRepairScreenPreview() {
    SparrowTheme {
        IdentityRepairScreen(uiState = IdentityRepairUiState(), onUiEvent = {})
    }
}
