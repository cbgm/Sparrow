package com.cbgm.sparrow.feature.identity.presentation.restore.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.feature.identity.presentation.restore.model.IdentityRestoreUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_identity_backup_password
import com.cbgm.sparrow.resources.feature_identity_backup_restore_action
import com.cbgm.sparrow.resources.feature_identity_backup_restore_hint
import org.jetbrains.compose.resources.stringResource

@Composable
fun IdentityRestoreDialog(
    state: IdentityRestoreUiState,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_identity_backup_restore_action),
        text = {
            Column {
                Text(stringResource(Res.string.feature_identity_backup_restore_hint))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(Res.string.feature_identity_backup_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            SparrowApprovalButton(
                enabled = state.canConfirm,
                onClick = onConfirm,
                text = stringResource(Res.string.feature_identity_backup_restore_action),
                fillMaxWidth = false
            )
        },
        dismissButton = {
            SparrowOutlinedButton(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        },
        isVisible = state.isDialogVisible
    )
}
