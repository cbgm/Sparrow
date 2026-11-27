package com.cbgm.sparrow.feature.identity.presentation.repair

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowDestructiveButton
import com.cbgm.sparrow.core.ui.component.SparrowDialogAction
import com.cbgm.sparrow.core.ui.component.SparrowInputField
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.asTint
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.identity.presentation.repair.model.IdentityRepairStageUi
import com.cbgm.sparrow.feature.identity.presentation.repair.model.IdentityRepairUiEvent
import com.cbgm.sparrow.feature.identity.presentation.repair.model.IdentityRepairUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_identity_backup_password_label
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(Dimens.ShareIdentityScreen.avatarSize)
                .background(MaterialTheme.colorScheme.error.asTint(Alpha.Button.destructive), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(Dimens.ShareIdentityScreen.actionIconContainerSize),
                tint = MaterialTheme.colorScheme.error
            )
        }
        Text(
            text = stringResource(Res.string.feature_identity_repair_title),
            modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(Res.string.feature_identity_repair_message),
            modifier = Modifier.padding(top = MaterialTheme.spacing.small),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (uiState.isBusy) {
            CircularProgressIndicator(modifier = Modifier.padding(top = MaterialTheme.spacing.large))
        }
        Spacer(Modifier.weight(1f))
        SparrowApprovalButton(
            onClick = { onUiEvent(IdentityRepairUiEvent.RestoreClicked) },
            text = stringResource(Res.string.feature_identity_repair_restore),
            enabled = !uiState.isBusy
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        SparrowDestructiveButton(
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
                SparrowInputField(
                    value = uiState.password,
                    onValueChange = { onUiEvent(IdentityRepairUiEvent.PasswordChanged(it)) },
                    label = stringResource(Res.string.feature_identity_backup_password_label),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isSingleLine = true
                )
            }
        },
        confirmButton = {
            SparrowDialogAction(
                enabled = uiState.canConfirmPassword,
                onClick = { onUiEvent(IdentityRepairUiEvent.PasswordConfirmed) },
                text = stringResource(Res.string.feature_identity_repair_restore)
            )
        },
        dismissButton = {
            SparrowDialogAction(
                onClick = { onUiEvent(IdentityRepairUiEvent.PasswordDismissed) },
                text = stringResource(Res.string.base_cancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            SparrowDialogAction(
                onClick = { onUiEvent(IdentityRepairUiEvent.ResetConfirmed) },
                text = stringResource(Res.string.feature_identity_repair_reset_confirm),
                color = MaterialTheme.colorScheme.error
            )
        },
        dismissButton = {
            SparrowDialogAction(
                onClick = { onUiEvent(IdentityRepairUiEvent.ResetDismissed) },
                text = stringResource(Res.string.base_cancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
